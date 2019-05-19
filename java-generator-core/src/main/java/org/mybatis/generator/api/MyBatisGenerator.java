/**
 *    Copyright 2006-2016 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.generator.api;

import static org.mybatis.generator.internal.util.ClassloaderUtility.getCustomClassloader;
import static org.mybatis.generator.internal.util.JavaBeansUtil.getCamelCaseString;
import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;
import static org.mybatis.generator.internal.util.messages.Messages.getString;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mybatis.generator.api.dom.java.CompilationUnit;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.codegen.RootClassInfo;
import org.mybatis.generator.codegen.freemarker.JavaDomainGenerator;
import org.mybatis.generator.codegen.freemarker.JavaServiceGenerator;
import org.mybatis.generator.codegen.freemarker.TemplateEntity.DomainTemplateEntity;
import org.mybatis.generator.codegen.freemarker.TemplateEntity.ServiceTemplateEntity;
import org.mybatis.generator.config.*;
import org.mybatis.generator.exception.InvalidConfigurationException;
import org.mybatis.generator.exception.ShellException;
import org.mybatis.generator.internal.*;

/**
 * This class is the main interface to MyBatis generator. A typical execution of the tool involves these steps:
 * 
 * <ol>
 * <li>Create a Configuration object. The Configuration can be the result of a parsing the XML configuration file, or it
 * can be created solely in Java.</li>
 * <li>Create a MyBatisGenerator object</li>
 * <li>Call one of the generate() methods</li>
 * </ol>
 *
 * @author Jeff Butler
 * @see org.mybatis.generator.config.xml.ConfigurationParser
 */
public class MyBatisGenerator {

    /** The configuration. */
    private Configuration configuration;

    /** The shell callback. */
    private ShellCallback shellCallback;

    /** The generated java files. */
    private List<GeneratedJavaFile> generatedJavaFiles;

    /** The generated xml files. */
    private List<GeneratedXmlFile> generatedXmlFiles;

    /** The warnings. */
    private List<String> warnings;

    /** The projects. */
    private Set<String> projects;

    /**
     * Constructs a MyBatisGenerator object.
     * 
     * @param configuration
     *            The configuration for this invocation
     * @param shellCallback
     *            an instance of a ShellCallback interface. You may specify
     *            <code>null</code> in which case the DefaultShellCallback will
     *            be used.
     * @param warnings
     *            Any warnings generated during execution will be added to this
     *            list. Warnings do not affect the running of the tool, but they
     *            may affect the results. A typical warning is an unsupported
     *            data type. In that case, the column will be ignored and
     *            generation will continue. You may specify <code>null</code> if
     *            you do not want warnings returned.
     * @throws InvalidConfigurationException
     *             if the specified configuration is invalid
     */
    public MyBatisGenerator(Configuration configuration, ShellCallback shellCallback,
            List<String> warnings) throws InvalidConfigurationException {
        super();
        if (configuration == null) {
            throw new IllegalArgumentException(getString("RuntimeError.2")); //$NON-NLS-1$
        } else {
            this.configuration = configuration;
        }

        if (shellCallback == null) {
            this.shellCallback = new DefaultShellCallback(false);
        } else {
            this.shellCallback = shellCallback;
        }

        if (warnings == null) {
            this.warnings = new ArrayList<String>();
        } else {
            this.warnings = warnings;
        }
        generatedJavaFiles = new ArrayList<GeneratedJavaFile>();
        generatedXmlFiles = new ArrayList<GeneratedXmlFile>();
        projects = new HashSet<String>();

        this.configuration.validate();
    }

    /**
     * This is the main method for generating code. This method is long running, but progress can be provided and the
     * method can be canceled through the ProgressCallback interface. This version of the method runs all configured
     * contexts.
     *
     * @param callback
     *            an instance of the ProgressCallback interface, or <code>null</code> if you do not require progress
     *            information
     * @throws SQLException
     *             the SQL exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws InterruptedException
     *             if the method is canceled through the ProgressCallback
     */
    public void generate(ProgressCallback callback) throws SQLException,
            IOException, InterruptedException {
        generate(callback, null, null, true);
    }

    /**
     * This is the main method for generating code. This method is long running, but progress can be provided and the
     * method can be canceled through the ProgressCallback interface.
     *
     * @param callback
     *            an instance of the ProgressCallback interface, or <code>null</code> if you do not require progress
     *            information
     * @param contextIds
     *            a set of Strings containing context ids to run. Only the contexts with an id specified in this list
     *            will be run. If the list is null or empty, than all contexts are run.
     * @throws SQLException
     *             the SQL exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws InterruptedException
     *             if the method is canceled through the ProgressCallback
     */
    public void generate(ProgressCallback callback, Set<String> contextIds)
            throws SQLException, IOException, InterruptedException {
        generate(callback, contextIds, null, true);
    }

    /**
     * This is the main method for generating code. This method is long running, but progress can be provided and the
     * method can be cancelled through the ProgressCallback interface.
     *
     * @param callback
     *            an instance of the ProgressCallback interface, or <code>null</code> if you do not require progress
     *            information
     * @param contextIds
     *            a set of Strings containing context ids to run. Only the contexts with an id specified in this list
     *            will be run. If the list is null or empty, than all contexts are run.
     * @param fullyQualifiedTableNames
     *            a set of table names to generate. The elements of the set must be Strings that exactly match what's
     *            specified in the configuration. For example, if table name = "foo" and schema = "bar", then the fully
     *            qualified table name is "foo.bar". If the Set is null or empty, then all tables in the configuration
     *            will be used for code generation.
     * @throws SQLException
     *             the SQL exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws InterruptedException
     *             if the method is canceled through the ProgressCallback
     */
    public void generate(ProgressCallback callback, Set<String> contextIds,
            Set<String> fullyQualifiedTableNames) throws SQLException,
            IOException, InterruptedException {
        generate(callback, contextIds, fullyQualifiedTableNames, true);
    }

    /**
     * This is the main method for generating code. This method is long running, but progress can be provided and the
     * method can be cancelled through the ProgressCallback interface.
     *
     * @param callback
     *            an instance of the ProgressCallback interface, or <code>null</code> if you do not require progress
     *            information
     * @param contextIds
     *            a set of Strings containing context ids to run. Only the contexts with an id specified in this list
     *            will be run. If the list is null or empty, than all contexts are run.
     * @param fullyQualifiedTableNames
     *            a set of table names to generate. The elements of the set must be Strings that exactly match what's
     *            specified in the configuration. For example, if table name = "foo" and schema = "bar", then the fully
     *            qualified table name is "foo.bar". If the Set is null or empty, then all tables in the configuration
     *            will be used for code generation.
     * @param writeFiles
     *            if true, then the generated files will be written to disk.  If false,
     *            then the generator runs but nothing is written
     * @throws SQLException
     *             the SQL exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws InterruptedException
     *             if the method is canceled through the ProgressCallback
     */
    public void generate(ProgressCallback callback, Set<String> contextIds,
            Set<String> fullyQualifiedTableNames, boolean writeFiles) throws SQLException,
            IOException, InterruptedException {

        if (callback == null) {
            callback = new NullProgressCallback();
        }

        generatedJavaFiles.clear();
        generatedXmlFiles.clear();
        ObjectFactory.reset();
        RootClassInfo.reset();

        // calculate the contexts to run
        List<Context> contextsToRun;
        if (contextIds == null || contextIds.size() == 0) {
            contextsToRun = configuration.getContexts();
        } else {
            contextsToRun = new ArrayList<Context>();
            for (Context context : configuration.getContexts()) {
                if (contextIds.contains(context.getId())) {
                    contextsToRun.add(context);
                }
            }
        }

        // setup custom classloader if required
        if (configuration.getClassPathEntries().size() > 0) {
            ClassLoader classLoader = getCustomClassloader(configuration.getClassPathEntries());
            ObjectFactory.addExternalClassLoader(classLoader);
        }

        // now run the introspections...
        int totalSteps = 0;
        for (Context context : contextsToRun) {
            totalSteps += context.getIntrospectionSteps();
        }
        callback.introspectionStarted(totalSteps);

        for (Context context : contextsToRun) {
            context.introspectTables(callback, warnings,
                    fullyQualifiedTableNames);
        }

        // now run the generates
        totalSteps = 0;
        for (Context context : contextsToRun) {
            totalSteps += context.getGenerationSteps();
        }
        callback.generationStarted(totalSteps);

        for (Context context : contextsToRun) {
            context.generateFiles(callback, generatedJavaFiles,
                    generatedXmlFiles, warnings);
        }
        /**
         * 如果JavaServiceGeneratorConfiguration存在则调用相关方法
         * 如果JavaDomainGeneratorConfiguration存在则调用相关方法
         */

        for (Context c:configuration.getContexts()) {
            if (c.getJavaServiceGeneratorConfiguration() != null) {

                JavaServiceGenerator.addJavaServiceGenerator(assignmentServiceTemplateEntity());
            }

            if (c.getJavaDomainGeneratorConfiguration() != null) {
                JavaDomainGenerator.addJavaDomainGenerator(assignmentDomainTemplateEntity());
            }
        }

        // now save the files
        if (writeFiles) {
            callback.saveStarted(generatedXmlFiles.size()
                + generatedJavaFiles.size());

            for (GeneratedXmlFile gxf : generatedXmlFiles) {
                projects.add(gxf.getTargetProject());
                writeGeneratedXmlFile(gxf, callback);

            }

            for (GeneratedJavaFile gjf : generatedJavaFiles) {
            	String shortName = gjf.getCompilationUnit().getType().getShortName();
                projects.add(gjf.getTargetProject());
                writeGeneratedJavaFile(gjf, callback);
                //  初始化 一个继承父类的 Mapper
                if(shortName.indexOf("Mapper") != -1) {
                	writeGeneratedJavaFileExtendBaseMapper(gjf, callback);
                }
            }

            for (String project : projects) {
                shellCallback.refreshProject(project);
            }
        }

        callback.done();
    }

    private void writeGeneratedJavaFile(GeneratedJavaFile gjf, ProgressCallback callback)
            throws InterruptedException, IOException {
        File targetFile;
        String source;
        try {
            File directory = shellCallback.getDirectory(gjf
                    .getTargetProject(), gjf.getTargetPackage());
            String mapperFileName = gjf.getFileName();
            String originalName = mapperFileName;
            
            // 文件名也加上Base
            if(mapperFileName.indexOf("Mapper")!=-1) {
            	mapperFileName ="Base"+mapperFileName;
            	targetFile = new File(directory,mapperFileName);            	
            	File mapperFile = new File(directory,originalName);
            	if(targetFile.exists() && mapperFile.exists()) { //    合并 需要先将Mapper 和 BaseMapper 合并，然后在让mybatis生成的mapper和BaseMapper 合并(343 line)
            		String cont = new JavaFileMergerJaxp().getNewJavaFileByAllFullPath(mapperFile.getAbsolutePath(),targetFile.getAbsolutePath());
            	 	writeFile(targetFile, cont, gjf.getFileEncoding());
            	}else {
            		
            	}
            }else {
            	targetFile = new File(directory,mapperFileName);
            }
            
            if (targetFile.exists()) {
                if (shellCallback.isMergeSupported()) {
//                    source = shellCallback.mergeJavaFile(gjf
//                            .getFormattedContent(), targetFile
//                            .getAbsolutePath(),
//                            MergeConstants.OLD_ELEMENT_TAGS,
//                            gjf.getFileEncoding());//多个设置编码
                    source = shellCallback.mergeJavaFile(gjf
                            .getFormattedContent(), targetFile
                            .getAbsolutePath());

                } else if (shellCallback.isOverwriteEnabled()) {
                    source = gjf.getFormattedContent();
                    warnings.add(getString("Warning.11", //$NON-NLS-1$
                            targetFile.getAbsolutePath()));
                } else {
                    source = gjf.getFormattedContent();
                    targetFile = getUniqueFileName(directory, gjf
                            .getFileName());
                    warnings.add(getString(
                            "Warning.2", targetFile.getAbsolutePath())); //$NON-NLS-1$
                }
            } else {
                source = gjf.getFormattedContent();
            }
            
            if(mapperFileName.indexOf("Mapper")!=-1) {
        		source=source.replace(originalName.replace(".java", ""), mapperFileName.replace(".java", ""));
        	}

            callback.checkCancel();
            callback.startTask(getString(
                    "Progress.15", targetFile.getName())); //$NON-NLS-1$
            writeFile(targetFile, source, gjf.getFileEncoding());
        } catch (ShellException e) {
            warnings.add(e.getMessage());
        }
    }
    
    
    private void writeGeneratedJavaFileExtendBaseMapper(GeneratedJavaFile gjf, ProgressCallback callback)
            throws InterruptedException, IOException {
        File targetFile;
        String source;
        try {
            File directory = shellCallback.getDirectory(gjf
                    .getTargetProject(), gjf.getTargetPackage());
            String mapperFileName = gjf.getFileName();
            
            targetFile = new File(directory,mapperFileName);
            // 文件名也加上Base
            if(mapperFileName.indexOf("Mapper")!=-1) {
            	mapperFileName ="Base"+mapperFileName;
            	  
//                Interface mapperInterface = new Interface(gjf.getTargetPackage() + "." + originalName);
            	 Interface mapperInterface = (Interface) gjf.getCompilationUnit();
              	
  	             mapperInterface.setVisibility(JavaVisibility.PUBLIC);
  	             mapperInterface.addJavaDocLine("/**");
  	             mapperInterface.addJavaDocLine(" * MyBatis Generator工具自动生成");
  	             mapperInterface.addJavaDocLine(" */");
  	             String superType = mapperFileName.replace(".java", "");
  	             FullyQualifiedJavaType daoSuperType = new FullyQualifiedJavaType(superType);
  	            
  	             mapperInterface.addSuperInterface(daoSuperType);
  	             
  	             List<Method>  methods =   mapperInterface.getMethods();
  	             methods.clear();
  	       
                 source = gjf.getFormattedContent();

                 callback.checkCancel();
                 callback.startTask(getString(
                          "Progress.15", targetFile.getName())); //$NON-NLS-1$
                 writeFile(targetFile, source, gjf.getFileEncoding());
            	}
              } catch (ShellException e) {
                  warnings.add(e.getMessage());
              }
            	 
    }

    private void writeGeneratedXmlFile(GeneratedXmlFile gxf, ProgressCallback callback)
            throws InterruptedException, IOException {
        File targetFile;
        String source;
        try {
            File directory = shellCallback.getDirectory(gxf
                    .getTargetProject(), gxf.getTargetPackage());
            targetFile = new File(directory, gxf.getFileName());
            if (targetFile.exists()) {
                if (gxf.isMergeable()) {
                    source = XmlFileMergerJaxp.getMergedSource(gxf,
                            targetFile);
                } else if (shellCallback.isOverwriteEnabled()) {
                    source = gxf.getFormattedContent();
                    warnings.add(getString("Warning.11", //$NON-NLS-1$
                            targetFile.getAbsolutePath()));
                } else {
                    source = gxf.getFormattedContent();
                    targetFile = getUniqueFileName(directory, gxf
                            .getFileName());
                    warnings.add(getString(
                            "Warning.2", targetFile.getAbsolutePath())); //$NON-NLS-1$
                }
            } else {
                source = gxf.getFormattedContent();
            }

            callback.checkCancel();
            callback.startTask(getString(
                    "Progress.15", targetFile.getName())); //$NON-NLS-1$
            writeFile(targetFile, source, "UTF-8"); //$NON-NLS-1$
        } catch (ShellException e) {
            warnings.add(e.getMessage());
        }
    }
    
    /**
     * Writes, or overwrites, the contents of the specified file.
     *
     * @param file
     *            the file
     * @param content
     *            the content
     * @param fileEncoding
     *            the file encoding
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void writeFile(File file, String content, String fileEncoding) throws IOException {

        FileOutputStream fos = new FileOutputStream(file, false);
        OutputStreamWriter osw;
        if (fileEncoding == null) {
            osw = new OutputStreamWriter(fos);
        } else {
            osw = new OutputStreamWriter(fos, fileEncoding);
        }
        
        BufferedWriter bw = new BufferedWriter(osw);
        bw.write(content);
        bw.close();
    }

    /**
     * Gets the unique file name.
     *
     * @param directory
     *            the directory
     * @param fileName
     *            the file name
     * @return the unique file name
     */
    private File getUniqueFileName(File directory, String fileName) {
        File answer = null;

        // try up to 1000 times to generate a unique file name
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < 1000; i++) {
            sb.setLength(0);
            sb.append(fileName);
            sb.append('.');
            sb.append(i);

            File testFile = new File(directory, sb.toString());
            if (!testFile.exists()) {
                answer = testFile;
                break;
            }
        }

        if (answer == null) {
            throw new RuntimeException(getString(
                    "RuntimeError.3", directory.getAbsolutePath())); //$NON-NLS-1$
        }

        return answer;
    }

    /**
     * Returns the list of generated Java files after a call to one of the generate methods.
     * This is useful if you prefer to process the generated files yourself and do not want
     * the generator to write them to disk.
     *  
     * @return the list of generated Java files
     */
    public List<GeneratedJavaFile> getGeneratedJavaFiles() {
        return generatedJavaFiles;
    }

    /**
     * Returns the list of generated XML files after a call to one of the generate methods.
     * This is useful if you prefer to process the generated files yourself and do not want
     * the generator to write them to disk.
     *  
     * @return the list of generated XML files
     */
    public List<GeneratedXmlFile> getGeneratedXmlFiles() {
        return generatedXmlFiles;
    }

    public List<ServiceTemplateEntity> assignmentServiceTemplateEntity(){
        List<Context> contexts = configuration.getContexts();
        List<ServiceTemplateEntity> serviceTemplateEntities = new ArrayList<ServiceTemplateEntity>();
        boolean flag;
        for (Context c:contexts){
            JavaServiceGeneratorConfiguration jgc = c.getJavaServiceGeneratorConfiguration();
            List<TableConfiguration> tableConfigurations = c.getTableConfigurations();
            for (TableConfiguration t:tableConfigurations){
                flag = false;
                for (GeneratedJavaFile gjf : generatedJavaFiles) {
                    if (gjf.getFileName().contains("WithBLOBs")&& gjf.getFileName().contains(t.getDomainObjectName())) {
                        flag = true;
                        break;
                    }
                }
                String domainObjectName = this.getDomainObjectName(t);
                ServiceTemplateEntity serviceTemplateEntity = new ServiceTemplateEntity();
                serviceTemplateEntity.setClassName(domainObjectName+"Service");
                String projectTargetPackage = jgc.getTargetProject()+"/"+jgc.getTargetPackage().replaceAll("\\.","/")+"/";
                serviceTemplateEntity.setProjectTargetPackage(projectTargetPackage);
                serviceTemplateEntity.setTemplatePackage(jgc.getTargetPackage());
                serviceTemplateEntity.setMapperType(domainObjectName+"Mapper");
                serviceTemplateEntity.setMapperName(Character.toLowerCase(domainObjectName.charAt(0)) + domainObjectName.substring(1)+"Mapper");
                serviceTemplateEntity.setModelClazz(domainObjectName);
                serviceTemplateEntity.setMapperPackage(c.getJavaClientGeneratorConfiguration().getTargetPackage()+"."+domainObjectName+"Mapper");
                serviceTemplateEntity.setModelPackage(c.getJavaModelGeneratorConfiguration().getTargetPackage()+"."+domainObjectName);
                serviceTemplateEntity.setColumnsHasBLOB(flag);
                serviceTemplateEntities.add(serviceTemplateEntity);
            }
        }
        return serviceTemplateEntities;
    }

    public List<DomainTemplateEntity> assignmentDomainTemplateEntity(){
        List<Context> contexts = configuration.getContexts();
        List<DomainTemplateEntity> domainTemplateEntityList = new ArrayList<DomainTemplateEntity>();
        for (Context c: contexts){
            JavaDomainGeneratorConfiguration jdc = c.getJavaDomainGeneratorConfiguration();
            List<TableConfiguration> tableConfigurations = c.getTableConfigurations();
            for (TableConfiguration t:tableConfigurations){
                String domainObjectName = this.getDomainObjectName(t);
                DomainTemplateEntity domainTemplateEntity = new DomainTemplateEntity();
                DomainTemplateEntity.DomainTemplate domainTemplate = new DomainTemplateEntity.DomainTemplate();
                DomainTemplateEntity.NativeDomainTemplate nativeDomainTemplate = new DomainTemplateEntity.NativeDomainTemplate();
                domainTemplate.setDomainPackage(jdc.getTargetPackage());
                domainTemplate.setBoPackage(c.getJavaBoGeneratorConfiguration().getTargetPackage()+"."+domainObjectName+"Bo");
                domainTemplate.setBoType(domainObjectName+"Bo");
                domainTemplate.setDomainInterface(domainObjectName+"Domain");
                domainTemplate.setProjectTargetPackage(jdc.getTargetProject()+"/"+jdc.getTargetPackage().replaceAll("\\.","/")+"/");
                nativeDomainTemplate.setNativeDomainPackage(jdc.getTargetPackage());
                nativeDomainTemplate.setBoPackage(c.getJavaBoGeneratorConfiguration().getTargetPackage()+"."+domainObjectName+"Bo");
                nativeDomainTemplate.setModelPackage(c.getJavaModelGeneratorConfiguration().getTargetPackage()+"."+domainObjectName);
                nativeDomainTemplate.setBoType(domainObjectName+"Bo");
                nativeDomainTemplate.setDomainInterface(domainObjectName+"Domain");
                String str = domainObjectName+"Domain";
                str = Character.toLowerCase(str.charAt(0)) + str.substring(1);
                nativeDomainTemplate.setDomainName(str);
                nativeDomainTemplate.setModelClazz(domainObjectName);
                nativeDomainTemplate.setModelServiceClazz(domainObjectName+"Service");
                str = domainObjectName+"Service";
                str = Character.toLowerCase(str.charAt(0)) + str.substring(1);
                nativeDomainTemplate.setModelServiceName(str);
                nativeDomainTemplate.setProjectTargetPackage(jdc.getTargetProject()+"/"+jdc.getTargetPackage().replaceAll("\\.","/")+"/");
                nativeDomainTemplate.setModelServicePackage(c.getJavaServiceGeneratorConfiguration().getTargetPackage()+"."+domainObjectName+"Service");
                nativeDomainTemplate.setNativeDomainClazz(domainObjectName+"NativeDomain");
                nativeDomainTemplate.setDomainPackage(domainTemplate.getDomainPackage()+"."+domainTemplate.getDomainInterface());
                domainTemplateEntity.setDomainTemplate(domainTemplate);
                domainTemplateEntity.setNativeDomainTemplate(nativeDomainTemplate);
                domainTemplateEntityList.add(domainTemplateEntity);
            }
        }
        return domainTemplateEntityList;
    }
    public String getDomainObjectName(TableConfiguration t) {
        if (stringHasValue(t.getDomainObjectName())) {
            return t.getDomainObjectName();
        } else {
            return getCamelCaseString(t.getTableName(), true);
        }
    }

}
