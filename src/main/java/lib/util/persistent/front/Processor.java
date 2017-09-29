/* Copyright (C) 2017  Intel Corporation
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 only, as published by the Free Software Foundation.
 * This file has been designated as subject to the "Classpath"
 * exception as provided in the LICENSE file that accompanied
 * this code.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License version 2 for more details (a copy
 * is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General Public License
 * version 2 along with this program; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

package lib.util.persistent.front;

import javax.annotation.processing.*;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.Element;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import javax.lang.model.SourceVersion;
import java.util.List;
import javax.tools.Diagnostic;
import lib.util.persistent.types.Types;

@SupportedAnnotationTypes("lib.util.persistent.front.PersistentClass")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class Processor extends AbstractProcessor {

    public Processor() {
        super();
    }

    @Override
    public boolean process(Set<? extends TypeElement> elements, RoundEnvironment env) {
        TypeElement persistentObjectType = processingEnv.getElementUtils().getTypeElement("lib.util.persistent.PersistentObject");
        TypeElement objectPointerType = processingEnv.getElementUtils().getTypeElement("lib.util.persistent.ObjectPointer");
        TypeElement objectTypeType = processingEnv.getElementUtils().getTypeElement("lib.util.persistent.types.ObjectType");
        TypeElement valueTypeType = processingEnv.getElementUtils().getTypeElement("lib.util.persistent.types.ValueType");
        for (Element element : env.getElementsAnnotatedWith(PersistentClass.class)) {
            TypeElement classElement = findEnclosingTypeElement(element);
            if (classElement.asType().toString().endsWith(">")) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, classElement + ", " + "checking of generic classes not yet supported, skipping");
                continue;
            }
            // System.out.println("checking " + classElement);
            Set<Modifier> classModifiers = classElement.getModifiers();
            boolean classIsFinal = classModifiers.contains(Modifier.FINAL);
            TypeMirror baseClass = classElement.asType();
            TypeElement superClass = null;
            while (!baseClass.equals(persistentObjectType.asType())) {
                superClass = processingEnv.getElementUtils().getTypeElement(baseClass.toString());
                if (superClass == null) {
                    error(processingEnv, classElement, "class must directly or indirectly extend PersistentObject");
                    break;
                }
                baseClass = superClass.getSuperclass();
            }
            if (superClass == null) continue;
            TypeMirror superType = baseClass;
            TypeElement pointerType = null;
            if (superType.equals(persistentObjectType.asType())) pointerType = objectPointerType;
            TypeElement thisClassType = processingEnv.getElementUtils().getTypeElement(classElement.toString());
            DeclaredType pointerOfThis = null;
            if (classIsFinal) {
                pointerOfThis = processingEnv.getTypeUtils().getDeclaredType(pointerType, thisClassType.asType());
            }
            else {
                WildcardType typeBound = processingEnv.getTypeUtils().getWildcardType(thisClassType.asType(), null);
                pointerOfThis = processingEnv.getTypeUtils().getDeclaredType(pointerType, typeBound);
            }
            ExecutableElement reconstructor = findReconstructor(processingEnv, classElement, pointerOfThis);
            Name expectedName = processingEnv.getElementUtils().getName(Types.TYPE_FIELD_NAME);
            TypeElement typeType = superType.equals(persistentObjectType.asType()) ? objectTypeType : valueTypeType;
            VariableElement typeField;
            if (superType.equals(persistentObjectType.asType())) {
                DeclaredType typeOfThis = processingEnv.getTypeUtils().getDeclaredType(typeType, thisClassType.asType());
                typeField = findTypeField(processingEnv, classElement, expectedName, typeOfThis);
            }
            else if (superType.equals(persistentObjectType.asType())) {
                typeField = findTypeField(processingEnv, classElement, expectedName, typeType.asType());
            }
        }
        return true;
    }

    private static TypeElement findEnclosingTypeElement(Element e) {
        while (e != null && !(e instanceof TypeElement)) {
            e = e.getEnclosingElement();
        }
        return TypeElement.class.cast(e);
    }

    private static void error(ProcessingEnvironment procEnv, Element element, String message) {
        procEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, element + ", " + message);
    }

    private static void warning(ProcessingEnvironment procEnv, Element element, String message) {
        procEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, element + ", " + message);
    }

    @SuppressWarnings("unchecked")
    private static ExecutableElement findReconstructor(ProcessingEnvironment procEnv, TypeElement classElement, TypeMirror pointerParam) {
        for (ExecutableElement ctor : ElementFilter.constructorsIn(classElement.getEnclosedElements())) {
            List<VariableElement> cps = (List<VariableElement>)ctor.getParameters();
            // string compare not right?, need to do more formal compare of types
            if (cps.size() == 1 && cps.get(0).asType().toString().equals(pointerParam.toString())) {
                Set<Modifier> modifiers = ctor.getModifiers();
                if (!modifiers.contains(Modifier.PROTECTED)) error(procEnv, classElement, "constructor taking a " + pointerParam + " is not protected");
                return ctor;
            }
        }
        error(procEnv, classElement, "no constructor taking an " + pointerParam + " was found");
        return null;
    }

    private static VariableElement findTypeField(ProcessingEnvironment procEnv, TypeElement classElement, Name expectedName, TypeMirror expectedType) {
        Set<Modifier> classModifiers = classElement.getModifiers();
        for (VariableElement field : ElementFilter.fieldsIn(classElement.getEnclosedElements())) {
            if (!field.getSimpleName().equals(expectedName)) continue;
            if (!field.asType().toString().equals(expectedType.toString())) error(procEnv, classElement, "'type' field must have type " + expectedType);
            Set<Modifier> fieldModifiers = field.getModifiers();
            if (!classModifiers.contains(Modifier.FINAL) && !fieldModifiers.contains(Modifier.PUBLIC)) warning(procEnv, classElement, "non-final class should have public 'type' field");
            if (classModifiers.contains(Modifier.FINAL) && !fieldModifiers.contains(Modifier.PRIVATE)) warning(procEnv, classElement, "final class should have private 'type' field");
            if (!fieldModifiers.contains(Modifier.STATIC)) error(procEnv, classElement, "'type' field is not static");
            if (!fieldModifiers.contains(Modifier.FINAL)) warning(procEnv, classElement, "'type' field is not final");
            return field;
        }
        error(procEnv, classElement, "no public static 'type' " + expectedType + " field found");
        return null;
    }
}
