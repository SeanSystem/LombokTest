package com.huge.lombok.process;

import com.huge.lombok.annotation.Getter;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * @author Sean
 * @date 2019/09/20
 */
@SupportedAnnotationTypes("com.huge.lombok.annotation.Getter")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class GettterProcess extends AbstractProcessor {
    /**
     * Messager主要是用来在编译期打log用的
     */
    private Messager messager;
    /**
     * JavacTrees提供了待处理的抽象语法树
     */
    private JavacTrees trees;
    /**
     * TreeMaker封装了创建AST节点的一些方法
     */
    private TreeMaker treeMaker;
    /**
     * Names提供了创建标识符的方法
     */
    private Names names;
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> set = roundEnv.getElementsAnnotatedWith(Getter.class);
        set.forEach(element ->{
                    JCTree jcTree = trees.getTree(element);
                    jcTree.accept(new TreeTranslator(){
                        @Override
                        public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                            List<JCTree.JCVariableDecl> jcVariableDeclList = List.nil();
                            for(JCTree tree : jcClassDecl.defs){
                                if(tree.getKind().equals(Tree.Kind.VARIABLE)){
                                    JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl)tree;
                                    jcVariableDeclList = jcVariableDeclList.append(jcVariableDecl);
                                }
                            }
                            jcVariableDeclList.forEach(jcVariableDecl -> {
                                messager.printMessage(Diagnostic.Kind.NOTE, jcVariableDecl.getName()+"has benn processed");
                                jcClassDecl.defs = jcClassDecl.defs.prepend(makeGetterMethodDecl(jcVariableDecl));
                            });
                            super.visitClassDef(jcClassDecl);
                        }
                    });
                }
                );
        return true;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.trees = JavacTrees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    private JCTree.JCMethodDecl makeGetterMethodDecl(JCTree.JCVariableDecl jcVariableDecl) {

        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
        statements.append(treeMaker.Return(treeMaker.Select(treeMaker.Ident(names.fromString("this")), jcVariableDecl.getName())));
        JCTree.JCBlock body = treeMaker.Block(0, statements.toList());
        return treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC), getNewMethodName(jcVariableDecl.getName()), jcVariableDecl.vartype, List.nil(), List.nil(), List.nil(), body, null);
    }

    private Name getNewMethodName(Name name) {
        String s = name.toString();
        return names.fromString("get" + s.substring(0, 1).toUpperCase() + s.substring(1, name.length()));
    }
}
