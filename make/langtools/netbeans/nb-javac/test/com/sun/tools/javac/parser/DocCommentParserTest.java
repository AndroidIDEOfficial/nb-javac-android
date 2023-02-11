package openjdk.tools.javac.parser;

import openjdk.source.doctree.DocCommentTree;
import openjdk.source.doctree.DocTree;
import openjdk.source.tree.ClassTree;
import openjdk.source.tree.CompilationUnitTree;
import openjdk.source.util.DocSourcePositions;
import openjdk.source.util.DocTreeScanner;
import openjdk.source.util.DocTrees;
import openjdk.source.util.TreePath;
import openjdk.tools.javac.api.JavacTaskImpl;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jdkx.tools.JavaCompiler;
import jdkx.tools.JavaFileObject;
import jdkx.tools.SimpleJavaFileObject;
import jdkx.tools.ToolProvider;
import junit.framework.TestCase;
import org.junit.Ignore;

/**
 *
 * @author lahvac
 */
public class DocCommentParserTest extends TestCase {
    
    public DocCommentParserTest(String testName) {
        super(testName);
    }
    
    public void testErrorRecovery1() throws IOException {
        doTestErrorRecovery("{@link\n" +
                            "{@link Object\n" +
                            "{@link Object#\n" +
                            "{@link Object#wait(lo\n" +
                            "{@link Object#wait(long)\n" +
                            "@see\n" +
                            "@see Object\n" +
                            "@see Object#\n" +
                            "@see Object#wait(lo\n" +
                            "@see Object#wait(long)\n",
                            "DOC_COMMENT:{@link\n" +
                            "{@link Object\n" +
                            "{@link Object#\n" +
                            "{@link Object#wait(lo\n" +
                            "{@link Object#wait(long)\n" +
                            "@see\n" +
                            "@see Object\n" +
                            "@see Object#\n" +
                            "@see Object#wait(lo\n" +
                            "@see Object#wait(long)",
                            "LINK:{@link\n",
                            "REFERENCE:",
                            "LINK:{@link Object\n",
                            "REFERENCE:Object",
                            "LINK:{@link Object#\n",
                            "REFERENCE:Object#",
                            "LINK:{@link Object#wait(lo\n",
                            "REFERENCE:Object#wait(lo\n",
                            "LINK:{@link Object#wait(long)\n",
                            "REFERENCE:Object#wait(long)",
                            "SEE:@see",
                            "SEE:@see Object",
                            "REFERENCE:Object",
                            "SEE:@see Object#",
                            "REFERENCE:Object#",
                            "SEE:@see Object#wait(lo\n",
                            "REFERENCE:Object#wait(lo\n",
                            "SEE:@see Object#wait(long)",
                            "REFERENCE:Object#wait(long)"
                           );
    }
    
    public void testErrorRecoveryValue() throws IOException {
        doTestErrorRecovery("{@value Math#PI\n" +
                            "@see Object#wait(long)\n",
                            "DOC_COMMENT:{@value Math#PI\n" +
                            "@see Object#wait(long)",
                            "VALUE:{@value Math#PI\n",
                            "REFERENCE:Math#PI",
                            "SEE:@see Object#wait(long)",
                            "REFERENCE:Object#wait(long)"
                           );
    }
    @Ignore
    public void test229748() throws IOException {
        doTestErrorRecovery("{@literal http://wikis.sun.com/display/mlvm/ProjectCoinProposal\n" +
                            "@see String\n",
                            "DOC_COMMENT:{@literal http://wikis.sun.com/display/mlvm/ProjectCoinProposal\n" +
                            "@see String",
                            "LITERAL:{@literal http://wikis.sun.com/display/mlvm/ProjectCoinProposal\n",
                            "TEXT:http://wikis.sun.com/display/mlvm/ProjectCoinProposal\n",
                            "SEE:@see String",
                            "REFERENCE:String");
    }
    
    public void test229725() throws IOException {
        doTestErrorRecovery("{@link http://wikis.sun.com/display/mlvm/ProjectCoinProposal}\n" +
                            "@see http://wikis.sun.com/display/mlvm/ProjectCoinProposal\n",
                            "DOC_COMMENT:{@link http://wikis.sun.com/display/mlvm/ProjectCoinProposal}\n" +
                            "@see http://wikis.sun.com/display/mlvm/ProjectCoinProposal",
                            "LINK:{@link http://wikis.sun.com/display/mlvm/ProjectCoinProposal}",
                            "REFERENCE:http://wikis.sun.com/display/mlvm/ProjectCoinProposal",
                            "SEE:@see http://wikis.sun.com/display/mlvm/ProjectCoinProposal",
                            "REFERENCE:http://wikis.sun.com/display/mlvm/ProjectCoinProposal"
                           );
    }
    
    public void testInlineSpan() throws IOException {
        doTestErrorRecovery("{@literal code}\n",
                            "DOC_COMMENT:{@literal code}\n",
                            "LITERAL:{@literal code}\n",
                            "TEXT:code"
                           );
    }
    
    private void doTestErrorRecovery(String javadocCode, String... golden) throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        final String code = "package test; /** " + javadocCode + " */public class Test {}";

        JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-Xjcov", "-XDkeepComments=true", "-XDbreakDocCommentParsingOnError=false"), null, Arrays.asList(new MyFileObject(code)));
        final CompilationUnitTree cut = ct.parse().iterator().next();
        DocTrees trees = DocTrees.instance(ct);
        final DocSourcePositions pos = trees.getSourcePositions();
        ClassTree clazz = (ClassTree) cut.getTypeDecls().get(0);
        final DocCommentTree dct = trees.getDocCommentTree(TreePath.getPath(cut, clazz));
        final List<String> result = new ArrayList<String>();
        
        new DocTreeScanner<Void, Object>() {
            @Override public Void scan(DocTree node, Object p) {
                if (node == null) return null;
                int start = (int) pos.getStartPosition(cut, dct, node);
                int end = (int) pos.getEndPosition(cut, dct, node);
                result.add(node.getKind() + ":" + code.substring(start, end));
                return super.scan(node, p);
            }
        }.scan(dct, null);
        
        assertEquals(Arrays.asList(golden), result);
    }
    private static class MyFileObject extends SimpleJavaFileObject {
        private String text;
        public MyFileObject(String text) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }
}
