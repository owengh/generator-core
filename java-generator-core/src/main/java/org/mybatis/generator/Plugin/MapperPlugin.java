package org.mybatis.generator.Plugin;


import java.util.ArrayList;
import java.util.List;
import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;
import org.mybatis.generator.api.GeneratedJavaFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.JavaFormatter;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.CompilationUnit;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaVisibility;

/**
 * 
 * @author wenjing.wang
 *
 */
public class MapperPlugin extends PluginAdapter{

    private static final String ROOT_SUPER_INTERFACE_CLASS = "com.fosunholiday.photo.mapper.BaseMapper";
    private String daoTargetDir;
    private String daoTargetPackage;

    private String rootSuperInterface;

    public MapperPlugin() {
    }

    /**
     * @param warnings
     * @return
     */
    public boolean validate(List<String> warnings) {
        daoTargetDir = properties.getProperty("targetProject");
        boolean valid = stringHasValue(daoTargetDir);

        daoTargetPackage = properties.getProperty("targetPackage");
        boolean valid2 = stringHasValue(daoTargetPackage);

        rootSuperInterface = properties.getProperty("daoSuperClass");
        if (!stringHasValue(rootSuperInterface)) {
        	rootSuperInterface = ROOT_SUPER_INTERFACE_CLASS;
        }

        return valid && valid2;
    }

    
	public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles(IntrospectedTable introspectedTable) {
        JavaFormatter javaFormatter = context.getJavaFormatter();
        List<GeneratedJavaFile> mapperJavaFiles = new ArrayList<GeneratedJavaFile>();
        for (GeneratedJavaFile javaFile : introspectedTable.getGeneratedJavaFiles()) {
            CompilationUnit unit = javaFile.getCompilationUnit();
            FullyQualifiedJavaType baseModelJavaType = unit.getType();

            String shortName = baseModelJavaType.getShortName();
 
            GeneratedJavaFile mapperJavafile = null;
            
            if(!shortName.endsWith("Mapper")) {
	            Interface mapperInterface = new Interface(daoTargetPackage + "." + shortName + "Mapper");
	
	            mapperInterface.setVisibility(JavaVisibility.PUBLIC);
	            mapperInterface.addJavaDocLine("/**");
	            mapperInterface.addJavaDocLine(" * MyBatis Generator工具自动生成");
	            mapperInterface.addJavaDocLine(" */");
	
	            FullyQualifiedJavaType daoSuperType = new FullyQualifiedJavaType(rootSuperInterface);
	            // 添加泛型支持
	            daoSuperType.addTypeArgument(baseModelJavaType);
	            mapperInterface.addImportedType(baseModelJavaType);
	            mapperInterface.addImportedType(daoSuperType);
	            //mapperInterface.addSuperInterface(daoSuperType);
	            mapperJavafile = new GeneratedJavaFile(mapperInterface, daoTargetDir, javaFormatter);
	            mapperJavaFiles.add(mapperJavafile);
            }
        }
        return mapperJavaFiles;
    }
	
}