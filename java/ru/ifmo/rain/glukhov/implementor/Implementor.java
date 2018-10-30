package ru.ifmo.rain.glukhov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.*;
import java.util.Objects;
import java.util.jar.*;

/**
 * This class implements {@link Impler} and {@link JarImpler}
 * @author antifrizz
 * @version 1.0
 * @see info.kgeorgiy.java.advanced.implementor.Impler
 * @see info.kgeorgiy.java.advanced.implementor.JarImpler
  */
public class Implementor implements Impler, JarImpler {
    /**
     * Print Errors with arguments from {@link #main(String[])}
     */
    private static void printErrorForMain() {
        System.err.println("Error: error with arguments");
    }

    /**
     * Main method designed for use {@link Implementor} via <tt>.jar</tt>
     * If you want to use {@link #implement(Class, Path)} you must give 2 arguments <tt>className root</tt>
     * If you want to use {@link #implementJar(Class, Path)} you must give 3 arguments <<tt>-jar className jarFile</tt>
     *
     * @param args arguments
     */
    public static void main(String args[]) {
        if (args == null) {
            printErrorForMain();
            return;
        }
        JarImpler implementor = new Implementor();
        try {
            if (args.length == 2 && args[0] != null) {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
                return;
            }
            if (args.length == 3 && args[0] != null && args[1] != null && args[2] != null && args[2].contains(".jar")) {
                if (args[0].equals("-jar")) {
                    implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
                } else {
                    printErrorForMain();
                }
            } else {
                printErrorForMain();
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Error: Class " + args[0] + " not found");
        } catch (ImplerException e) {
            System.err.println("Error at runtime for implementation: " + e.getMessage());
        }
    }

    /**
     * Check errors with arguments.
     *
     * @param token type token to create implementation for.
     * @param root root directory.
     * @throws info.kgeorgiy.java.advanced.implementor.ImplerException if some arguments are null or <tt>token</tt> isn't interface
     */
    private void checkArgs(Class<?> token, Path root) throws ImplerException{
        if (root == null && token == null) {
            throw new ImplerException("Error: token and path are null");
        }
        if (root == null) {
            throw new ImplerException("Error: path is null");
        }
        if (token == null) {
            throw new ImplerException("Error: token is null");
        }
        if (!token.isInterface()) {
            throw new ImplerException("Error: token isn't an interface");
        }
    }
    /**
     * Produces <tt>.jar</tt> file implementing class or interface specified by provided <tt>token</tt>.
     * <p>
     * Generated class full name should be same as full name of the type token with <tt>Impl</tt> suffix
     * added.
     *
     * @param token type token to create implementation for.
     * @param jarFile target <tt>.jar</tt> file.
     * @throws info.kgeorgiy.java.advanced.implementor.ImplerException if the class can't be generated for reasons like:
     * <ul>
     *     <li>Some argument was <tt>null</tt></li>
     *     <li><tt>token</tt> isn't interface</li>
     *     <li>Errors in runtime via {@link #implement(Class, Path)}</li>
     *     <li>Errors with creating files and directories</li>
     *     <li>Errors with deleting files and directories</li>
     *     <li>Errors in runtime via {@link JavaCompiler} when it compile generated via {@link #implement(Class, Path)} class</li>
     *     <li>I/O Errors</li>
     * </ul>
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        checkArgs(token, jarFile);
        Path root = FileSystems.getDefault().getPath("tmp");
        implement(token, root);
        Path folders = getPath(token, root);
        Path javaFile = folders.resolve(token.getSimpleName() + "Impl.java");
        Path classFile = folders.resolve(token.getSimpleName() + "Impl.class");
        String className = token.getPackage().getName().replace(".", "/")+ "/"
                + token.getSimpleName() + "Impl.class";

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try {
            if (compiler.run(null, null, null, javaFile.toString()) != 0) {
                throw new ImplerException("Error: can't compile java class");
            }
        }
        catch (NullPointerException e) {
            throw new ImplerException("Error: compilation error");
        }

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarFile), manifest);
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(classFile.toString())))
        {
            out.putNextEntry(new JarEntry(className));
            int blockSize = 2048;
            byte[] bytes = new byte[blockSize];
            while (in.available() > 0) {
                out.write(bytes, 0, in.read(bytes));
            }
            out.closeEntry();
        }
        catch (IOException e) {
            throw new ImplerException("Error: can't create .jar file in path jarFile");
        }

        try{
            deleteTmp(root.toFile());
        }
        catch (IOException e) {
            throw new ImplerException("Error: can't delete tmp file and directories");
        }
    }

    /**
     * Delete tmp files and directories
     *
     * @param file root directory for creating implementation class
     * @throws IOException If can't delete tmp file and directories
     */
    private void deleteTmp(File file) throws IOException {
        if (file.isDirectory()) {
            for (File i : Objects.requireNonNull(file.listFiles())) {
                deleteTmp(i);
            }
        }
        if (!file.delete()) {
            throw new IOException("");
        }
    }

    /**
     * Produces code implementing class or interface specified by provided <tt>token</tt>.
     * <p>
     * Generated class full name should be same as full name of the type token with <tt>Impl</tt> suffix
     * added. Generated source code should be placed in the correct subdirectory of the specified
     * <tt>root</tt> directory and have correct file name.
     *
     * @param token type token to create implementation for.
     * @param root root directory.
     * @throws info.kgeorgiy.java.advanced.implementor.ImplerException if the class can't be generated for reasons like:
     * <ul>
     *     <li>Some argument was <tt>null</tt></li>
     *     <li><tt>token</tt> isn't interface</li>
     *     <li>Errors with creating files and directories</li>
     *     <li>I/O Errors</li>
     * </ul>
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        checkArgs(token, root);
        PrintWriter writer = makeFile(token, getPath(token, root));
        printPackage(token, writer);
        printClassName(token, writer);
        printMethods(token, writer);
        writer.println(toUnicode("}"));
        writer.close();
    }

    /**
     * Make file and directories
     *
     * @param token type token to create implementation for.
     * @param root root directory with package directories.
     * @throws info.kgeorgiy.java.advanced.implementor.ImplerException if method can't make file or directories.
     */
    private PrintWriter makeFile(Class<?> token, Path root) throws ImplerException {
        try {
            Files.createDirectories(root);
            return new PrintWriter(Files.newBufferedWriter(root.resolve(token.getSimpleName() + "Impl.java")));
        } catch (IOException e) {
            throw new ImplerException("Error: can't create file");
        }
    }

    /**
     * Add to <tt>root</tt> directories from package.
     *
     * @param token type token to create implementation for.
     * @param root root directory.
     * @return {@link Path} <tt>root</tt> with included directories from package.
     */
    private Path getPath(Class<?> token, Path root) {
        return Paths.get(root.toString() + "/" + token.getPackage().getName().replace('.', '/') + "/");
    }

    /**
     * Print package of <tt>token</tt> implementing class via <tt>writer</tt>.
     *
     * @param token type token to create implementation for.
     * @param writer file descriptor in which need to print source code.
     */
    private void printPackage(Class<?> token, PrintWriter writer) {
        if (token.getPackage() != null) {
            writer.println(toUnicode("package " + token.getPackage().getName() + ";"));
        }
    }

    /**
     * Print class name of <tt>token</tt> implementing class via <tt>writer</tt>/
     *
     * @param token type token to create implementation for.
     * @param writer file descriptor in which need to print source code/
     */
    private void printClassName(Class<?> token, PrintWriter writer) {
        writer.println(toUnicode(getModifiers(token.getModifiers()) + "class " + token.getSimpleName() + "Impl implements " + token.getCanonicalName() + " {"));
    }

    /**
     * Return string representation of modifiers for <tt>mod</tt>.
     *
     * @param mod int value who contains modifiers.
     * @return {@link String} string representation of modifiers.
     */
    private String getModifiers(int mod) {
        return Modifier.toString(mod & ~Modifier.ABSTRACT & ~Modifier.INTERFACE & ~Modifier.TRANSIENT) + " ";
    }

    /**
     * Print annotation of <tt>method</tt> via <tt>writer</tt>.
     *
     * @param method method.
     * @param writer file descriptor in which need to print source code.
     */
    private void printAnnotation(Method method, PrintWriter writer) {
        for (Annotation a : method.getAnnotations()) {
            writer.println("@" + a.annotationType().getSimpleName());
        }
    }

    /**
     * Return string representation of parameters from <tt>method</tt>.
     *
     * @param method method.
     * @return {@link java.lang.String} string representation of parameters from <tt>method</tt>.
     */
    private String getParameters(Method method) {
        StringBuilder ans = new StringBuilder("(");
        Parameter[] a = method.getParameters();
        for (int i = 0; i < a.length; i++) {
            if (i == a.length - 1) {
                ans.append(a[i].getType().getCanonicalName()).append(" ").append(a[i].getName());
            } else {
                ans.append(a[i].getType().getCanonicalName()).append(" ").append(a[i].getName()).append(", ");
            }

        }
        ans.append(") ");
        return ans.toString();
    }

    /**
     * Return string representation of return type from <tt>method</tt>.
     *
     * @param method method.
     * @return {@link String} string representation of return type from <tt>method</tt>.
     */
    private String getReturn(Method method) {
        StringBuilder ans = new StringBuilder();
        if (method.getReturnType().equals(void.class)) {
            ans.append("");
        } else if (method.getReturnType().equals(boolean.class)) {
            ans.append(" false");
        } else if (method.getReturnType().isPrimitive()) {
            ans.append(" 0");
        } else {
            ans.append(" null");
        }
        return ans.toString();
    }

    /**
     * Return class name of throw for <tt>method</tt>
     * @param method method.
     * @return {@link String} string representation for throws class name for <tt>method</tt>
     */
    private String getThrows(Method method) {
        Class<?>[] a = method.getExceptionTypes();
        if (a.length == 0) {
            return "";
        } else {
            StringBuilder ans = new StringBuilder();
            ans.append(" throws ");
            for (int i = 0; i < a.length; i++) {
                if (i != a.length - 1) {
                    ans.append(a[i].getCanonicalName()).append(", ");
                } else {
                    ans.append(a[i].getCanonicalName()).append(" ");
                }
            }
            return ans.toString();
        }
    }

    /**
     * Print methods from <tt>token</tt> implementing class via <tt>writer</tt>.
     *
     * @param token type token to create implementation for.
     * @param writer file descriptor in which need to print source code.
     */
    private void printMethods(Class<?> token, PrintWriter writer) {
        for (Method method : token.getMethods()) {
            printAnnotation(method, writer);
            writer.println(toUnicode(getModifiers(method.getModifiers()) + method.getReturnType().getCanonicalName() + " " + method.getName() + getParameters(method) + getThrows(method) + "{ return" + getReturn(method) + "; }"));
        }
    }

    /**
     * Convert <tt>string</tt> to unicode string
     * @param string some string
     * @return {@link java.lang.String} unicode string
     */
    private String toUnicode(String string) {
        StringBuilder a = new StringBuilder();
        for (char i: string.toCharArray()) {
            if (i >= 128) {
                a.append("\\u").append(String.format("%04X", (int) i));
            } else {
                a.append(i);
            }

        }
        return a.toString();
    }
}
