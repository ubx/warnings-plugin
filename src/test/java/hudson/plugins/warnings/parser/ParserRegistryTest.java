package hudson.plugins.warnings.parser;

import static org.junit.Assert.*;
import hudson.plugins.analysis.core.ParserResult;
import hudson.plugins.analysis.util.model.FileAnnotation;
import hudson.plugins.warnings.GroovyParser;
import hudson.plugins.warnings.WarningsDescriptorTest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Tests the class {@link ParserRegistry}.
 */
public class ParserRegistryTest {
    private static final File DUMMY_FILE = new File("");
    /** Filename with all warnings. */
    private static final String FILE_NAME = "all.txt";
    /** Error message. */
    private static final String WRONG_NUMBER_OF_ANNOTATIONS_PARSED = "Wrong number of annotations parsed";

    /**
     * Checks whether we correctly find all Oracle invalids in the log file.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void testOracleInvalidsParser() throws IOException {
        List<WarningsParser> parsers = new ArrayList<WarningsParser>();
        parsers.add(new InvalidsParser());
        ParserRegistry parserRegistry = createRegistryUnderTest(FILE_NAME, StringUtils.EMPTY, StringUtils.EMPTY, parsers);

        Collection<FileAnnotation> annotations = parserRegistry.parse(DUMMY_FILE);
        Assert.assertEquals(WRONG_NUMBER_OF_ANNOTATIONS_PARSED, 2, annotations.size());

        ParserResult result = new ParserResult();
        result.addAnnotations(annotations);
        Assert.assertEquals(WRONG_NUMBER_OF_ANNOTATIONS_PARSED, 2, result.getNumberOfAnnotations());

        result.addAnnotations(annotations);
        Assert.assertEquals(WRONG_NUMBER_OF_ANNOTATIONS_PARSED, 2, result.getNumberOfAnnotations());
    }

    /**
     * Checks whether we correctly find all warnings of two parsers in the log file.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void testTwoParsers() throws IOException {
        List<WarningsParser> parsers = new ArrayList<WarningsParser>();
        parsers.add(new InvalidsParser());
        parsers.add(new JavaDocParser());
        ParserRegistry parserRegistry = createRegistryUnderTest(FILE_NAME, StringUtils.EMPTY, StringUtils.EMPTY, parsers);

        Collection<FileAnnotation> annotations = parserRegistry.parse(DUMMY_FILE);
        Assert.assertEquals(WRONG_NUMBER_OF_ANNOTATIONS_PARSED, 10, annotations.size());

        ParserResult result = new ParserResult();
        result.addAnnotations(annotations);
        Assert.assertEquals(WRONG_NUMBER_OF_ANNOTATIONS_PARSED, 10, result.getNumberOfAnnotations());

        result.addAnnotations(annotations);
        Assert.assertEquals(WRONG_NUMBER_OF_ANNOTATIONS_PARSED, 10, result.getNumberOfAnnotations());
    }

    /**
     * Checks whether we correctly find all warnings in the log file. The file
     * contains 18 additional ANT warnings, 8 of them should be excluded by the single
     * pattern.
     *
     * @throws IOException
     *             if the file could not be read
     * @see <a href="http://issues.jenkins-ci.org/browse/JENKINS-2359">Issue 2359</a>
     */
    @Test
    public void issue2359() throws IOException {
        ParserRegistry parserRegistry = createRegistryUnderTest(FILE_NAME, StringUtils.EMPTY, "/tmp/clover*/**", createJavaParsers());

        Collection<FileAnnotation> annotations = parserRegistry.parse(DUMMY_FILE);
        int excludedNumberOfWarnings = 8;
        Assert.assertEquals(WRONG_NUMBER_OF_ANNOTATIONS_PARSED, computeTotalNumberOfWarnings(createJavaParsers()) - excludedNumberOfWarnings, annotations.size());
    }

    /**
     * Checks that we do not count duplicates.
     *
     * @throws IOException
     *             if the file could not be read
     * @see <a href="http://issues.jenkins-ci.org/browse/JENKINS-7775">Issue 7775</a>
     */
    @Test
    public void issue7775() throws IOException {
        ParserRegistry parserRegistry = createRegistryUnderTest("issue7775.txt", StringUtils.EMPTY, StringUtils.EMPTY, Lists.newArrayList(new MsBuildParser()));

        Collection<FileAnnotation> annotations = parserRegistry.parse(DUMMY_FILE);
        Assert.assertEquals(WRONG_NUMBER_OF_ANNOTATIONS_PARSED,  57, annotations.size());
    }

    private List<WarningsParser> createJavaParsers() {
        List<WarningsParser> parsers = new ArrayList<WarningsParser>();
        parsers.add(new JavacParser());
        parsers.add(new AntJavacParser());
        return parsers;
    }

    private int computeTotalNumberOfWarnings(final List<WarningsParser> parsers) throws IOException {
        ParserRegistry parserRegistry = createRegistryUnderTest(FILE_NAME, StringUtils.EMPTY, StringUtils.EMPTY, parsers);

        return parserRegistry.parse(DUMMY_FILE).size();
    }

    /**
     * The file contains 18 additional ANT warnings, 15 of them should be excluded by the
     * two patterns.
     *
     * @throws IOException
     *             if the file could not be read
     * @see <a href="http://issues.jenkins-ci.org/browse/JENKINS-2359">Issue 2359</a>
     */
    @Test
    public void multiplePatternsIssue2359() throws IOException {
        ParserRegistry parserRegistry = createRegistryUnderTest(FILE_NAME, StringUtils.EMPTY, "/tmp/clover*/**, **/renderers/*", createJavaParsers());

        Collection<FileAnnotation> annotations = parserRegistry.parse(DUMMY_FILE);
        Assert.assertEquals(WRONG_NUMBER_OF_ANNOTATIONS_PARSED, computeTotalNumberOfWarnings(createJavaParsers()) - 15, annotations.size());
    }

    /**
     * Checks whether we correctly find all warnings in the log file. The file
     * contains 18 additional ANT warnings, 8 of them should be included by the single
     * pattern.
     *
     * @throws IOException
     *             if the file could not be read
     * @see <a href="http://issues.jenkins-ci.org/browse/JENKINS-3866">Issue 3866</a>
     */
    @Test
    public void issue3866() throws IOException {
        ParserRegistry parserRegistry = createRegistryUnderTest(FILE_NAME, "/tmp/clover*/**", StringUtils.EMPTY, new ArrayList<WarningsParser>());

        Collection<FileAnnotation> annotations = parserRegistry.parse(DUMMY_FILE);
        Assert.assertEquals(WRONG_NUMBER_OF_ANNOTATIONS_PARSED, 8, annotations.size());
    }

    /**
     * The file contains 18 additional ANT warnings, 15 of them should be included by the
     * two patterns.
     *
     * @throws IOException
     *             if the file could not be read
     * @see <a href="http://issues.jenkins-ci.org/browse/JENKINS-3866">Issue 3866</a>
     */
    @Test
    public void multiplePatternsIssue3866() throws IOException {
        ArrayList<WarningsParser> parsers = new ArrayList<WarningsParser>();
        parsers.add(new AntJavacParser());
        ParserRegistry parserRegistry = createRegistryUnderTest(FILE_NAME, "/tmp/clover*/**, **/renderers/*", StringUtils.EMPTY, parsers);

        Collection<FileAnnotation> annotations = parserRegistry.parse(DUMMY_FILE);
        Assert.assertEquals(WRONG_NUMBER_OF_ANNOTATIONS_PARSED, 15, annotations.size());
    }

    /**
     * The file contains 18 additional ANT warnings, 8 of them should be included by
     * one pattern, 7 of them should be excluded by one pattern.
     *
     * @throws IOException
     *             if the file could not be read
     * @see <a href="http://issues.jenkins-ci.org/browse/JENKINS-3866">Issue 3866</a>
     */
    @Test
    public void complexFilterIssue3866() throws IOException {
        ArrayList<WarningsParser> parsers = new ArrayList<WarningsParser>();
        parsers.add(new AntJavacParser());
        ParserRegistry parserRegistry = createRegistryUnderTest(FILE_NAME, "/tmp/clover*/**", "**/renderers/*", parsers);

        Collection<FileAnnotation> annotations = parserRegistry.parse(DUMMY_FILE);
        Assert.assertEquals(WRONG_NUMBER_OF_ANNOTATIONS_PARSED, 1, annotations.size());
    }

    /**
     * Creates the registry under test.
     *
     * @param fileName
     *            file name with the warnings
     * @param includePattern
     *            Ant file-set pattern of files to include in report,
     *            <code>null</code> or an empty string do not filter the input
     * @param excludePattern
     *            Ant file-set pattern of files to exclude from report,
     *            <code>null</code> or an empty string do not filter the output
     * @param parsers
     *            the parsers to use
     * @return the registry
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("SIC")
    private ParserRegistry createRegistryUnderTest(final String fileName, final String includePattern, final String excludePattern, final List<? extends WarningsParser> parsers) {
        ParserRegistry parserRegistry = new ParserRegistry(parsers, "", includePattern, excludePattern) {
            /** {@inheritDoc} */
            @Override
            protected Reader createReader(final File file) throws FileNotFoundException {
                return new InputStreamReader(ParserRegistryTest.class.getResourceAsStream(fileName));
            }
        };
        return parserRegistry;
    }

    /**
     * Verifies that illegal names are filtered.
     */
    @Test
    public void testFiltering() {
        String validName = JavacParser.WARNING_TYPE;
        List<String> filtered = ParserRegistry.filterExistingParserNames(Sets.newHashSet("Illegal", validName));

        assertEquals("Wrong number of filteres elements", 1, filtered.size());
        assertEquals("Wrong number of filteres elements", validName, filtered.get(0));
    }

    /**
     * Tests the construction of dynamic parsers.
     */
    @Test
    public void testDynamicParsers() {
        GroovyParser multi = new GroovyParser("name", WarningsDescriptorTest.MULTI_LINE_REGEXP, "empty");
        GroovyParser single = new GroovyParser("name", WarningsDescriptorTest.SINGLE_LINE_REGEXP, "empty");

        List<WarningsParser> allParsers = ParserRegistry.getDynamicParsers(Lists.newArrayList(single, multi));

        int multiNumber = 0;
        int singleNumber = 0;

        for (WarningsParser parser : allParsers) {
            if (parser.getClass() == DynamicParser.class) {
                singleNumber++;
            }
            else if (parser.getClass() == DynamicDocumentParser.class) {
                multiNumber++;
            }
        }

        assertEquals("Wrong number of single line parsers" , 1, singleNumber);
        assertEquals("Wrong number of multi line parsers" , 1, multiNumber);
    }
}

