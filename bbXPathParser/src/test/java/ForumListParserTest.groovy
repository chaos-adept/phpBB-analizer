import datavalue.forum.Topic
import groovy.util.slurpersupport.GPathResult
import org.apache.commons.lang3.StringUtils
import org.junit.Test;
import org.apache.xerces.xni.parser.XMLDocumentFilter
import org.cyberneko.html.parsers.SAXParser
import org.cyberneko.html.filters.Purifier

/**
 * chaos-adept
 * 08.01.2015.
 */
public class ForumListParserTest {

    def createAndSetParser() {
        SAXParser parser = new SAXParser()
        parser.setProperty("http://cyberneko.org/html/properties/filters", [new Purifier()] as XMLDocumentFilter[])
        parser.setProperty("http://cyberneko.org/html/properties/default-encoding", "Windows-1252")
        parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower")
        parser.setProperty("http://cyberneko.org/html/properties/names/attrs", "lower")
        parser.setFeature("http://cyberneko.org/html/features/scanner/normalize-attrs", true)
        parser.setFeature("http://cyberneko.org/html/features/scanner/ignore-specified-charset", true)
        parser.setFeature("http://cyberneko.org/html/features/override-doctype", false)
        parser.setFeature("http://cyberneko.org/html/features/override-namespaces", false)
        parser.setFeature("http://cyberneko.org/html/features/balance-tags", true)
        return new XmlSlurper(parser)
    }


    @Test
    void testBuildForumTaxonomy() {
        //given
        def txt = getClass().getResourceAsStream("main-page-Sevastopol.info.htm").text;
        def parser = createAndSetParser()
        def pageXml = parser.parseText(txt)

        //when
        def categories = extractCategoriesTree(pageXml);

        //then
        assert categories.collect({it.title}) == ['Форумы Севпортала', 'Горячие темы', 'Женский клуб', 'Развлечения', 'Полезное']
        assert categories[1].children.collect { it.title } == ['Политика', 'Городская Доска почета']
    }

    @Test
    void testExtractListOfTopics() {
        //given
        def txt = getClass().getResourceAsStream("primary-forum-page-Sevastopol.info.htm").text;
        def parser = createAndSetParser()
        def pageXml = parser.parseText(txt)

        //when
        def topics = extractTopics(pageXml);

        //then
        assert topics.collect({ it.title }).containsAll(['Защитим Парк Победы от застройки!!! Намечается новая!!',
                                                         'Стол находок', 'Пропал человек. Район Хрусталёва - Индустриальная'])
    }

    def extractTopics(pageXml) {
        def forumsTables = pageXml."**".findAll ( { it.@class == "tablebg" } )

        def topics = [];
        forumsTables.each { forumsTable ->
            def forumRows = forumsTable.tbody.tr;
            assert forumRows.size() > 0

            forumRows.each { row ->
                if (row?.td?.size() == 6) {

                    def topic = new Topic();
                    topic.title = row.td[1].a.toString().trim().split('\n').collect { it.trim() }.join(' ')
                    topics.add topic

                }
                if (row?.td?.size() == 2) {

                }
            }
        }

        topics;
    }

    def findForumsTable(pageXml) {
        def forumsTable = pageXml."**".find( { it.@class == "tablebg" && it."**".find({
            child -> child.toString() =~ 'Форум' }) } )

        forumsTable;
    }

    def extractCategoriesTree(pageXml) {
        def forumsTable = findForumsTable(pageXml)
        assert forumsTable

        def forumRows = forumsTable.tbody.tr;
        assert forumRows.size() > 0

        def categories = [];
        forumRows.each { row ->
            if (row?.td?.size() == 4) {
                //headers
            }
            if (row?.td?.size() == 2 ) {
                //category
                def categoryTopic = row.td.h4.a
                def cat = new datavalue.forum.Category()
                cat.id = categoryTopic.@href;
                cat.title = categoryTopic.toString().trim();

                categories.add cat
            }
            if (row?.td?.size() == 5) {
                def firstColumn = row.td.find { it.@class == 'row1' && it.@width == '100%' }
                def topLevelTopLink = firstColumn.a
                def cat = new datavalue.forum.Category()
                cat.id = topLevelTopLink.@href;
                cat.title = topLevelTopLink.toString().trim();

                categories.last().children.add cat;
            }
        }

        return categories;
    }
}
