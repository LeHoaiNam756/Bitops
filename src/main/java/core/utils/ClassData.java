package core.utils;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ClassData {
    private String typeOfClass;
    private String className;
    private String classModifier;
    private String superClassName;
    private List<String> superInterfaceName;
    private String fields = "";

    public ClassData(TypeDeclaration typeDeclaration) {
        if(typeDeclaration.isInterface()) {
            typeOfClass = "interface";
        } else {
            typeOfClass = "class";
        }
        className = typeDeclaration.getName().getIdentifier();
        int modifiers = typeDeclaration.getModifiers();
        switch (modifiers) {
            case Modifier.PUBLIC:
                classModifier = "public";
                break;
            case Modifier.PRIVATE:
                classModifier = "private";
                break;
            case Modifier.PROTECTED:
                classModifier = "protected";
                break;
            default:
                classModifier = "default";
                break;
        }
        if(typeDeclaration.getSuperclassType() != null) {
            superClassName = typeDeclaration.getSuperclassType().toString();
        }
        if(!typeDeclaration.superInterfaceTypes().isEmpty()) {
            List<?> interfaceList = typeDeclaration.superInterfaceTypes();
            superInterfaceName = new ArrayList<>();
            for (Object o : interfaceList) {
                superInterfaceName.add(o.toString());
            }
        }

        FieldDeclaration[] fieldDeclarations = typeDeclaration.getFields();
        StringBuilder extractedFields = new StringBuilder();
        for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
            extractedFields.append(fieldDeclaration).append("\n");
        }
        fields = extractedFields.toString();
    }
}
