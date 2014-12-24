package me.hatu.smzdm

import grizzled.slf4j.Logging
import org.htmlcleaner.HtmlCleaner
import com.huaban.analysis.jieba.JiebaSegmenter
import java.net.URL
import scala.collection.JavaConversions._
import com.sun.syndication.io._
import com.sun.syndication.feed.synd._
import com.typesafe.config._
import scala.collection.mutable.ListBuffer

object WebParser extends Logging {
	private val USER_AGENT : String = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:23.0) Gecko/20100101 Firefox/23.0";

	def trim(content: String) : String = {
		return content.replaceAll("""(?m)\s+$""", "").replaceAll("""(?m)^\s+""","")
	}

	def load_new_feeds: List[Article] = {
		var listBuffer = new ListBuffer[Article]

    try {
      val sfi = new SyndFeedInput()

      //val urls = List("http://feed.smzdm.com")
	    val urls = ConfigFactory.load().getStringList("smzdm.feeds").toList
      urls.foreach(url => debug(f"Add feed $url%s"))

      urls.foreach(url => {
        var conn = new URL(url).openConnection()
        conn.setRequestProperty("User-Agent", USER_AGENT)
        val feed = sfi.build(new XmlReader(conn))

        val entries = feed.getEntries()

        val list = entries.toList
                  .map( x => 
                    parse_article(trim(x.asInstanceOf[SyndEntryImpl].getTitle), 
                                  trim(x.asInstanceOf[SyndEntryImpl].getLink)) )
                  .filter ( x => x.valid == true )
        listBuffer ++= list

        debug(listBuffer.size)

      })


    } catch {
      case e : Throwable => throw new RuntimeException(e)
    }
    return listBuffer.toList
	}

	def parse_article(title: String, link: String) : Article = {
	    var article = new Article(title)
	    article.link = link

      debug(f"parse article: $link%s")

	    val cleaner = new HtmlCleaner
	    val props = cleaner.getProperties

      try {
        val lConn = new URL(link).openConnection();
        lConn.setRequestProperty("User-Agent", USER_AGENT)
        lConn.connect();
        val inputStream = lConn.getInputStream()

        val rootNode = cleaner.clean( inputStream)

      // content 
        val p_elements = rootNode.getElementsByName("p", true)
        var content = ""
        for (elem <- p_elements) {
          val itemprop = elem.getAttributeByName("itemprop")
          if (itemprop != null && itemprop.equalsIgnoreCase("description")) {
            content += elem.getText.toString
//          debug(elem.getText.toString)
          }
        }
        article.content = content

      // category
        val span_elements = rootNode.getElementsByName("span", true)
        for (elem <- span_elements) {
          val itemprop = elem.getAttributeByName("itemprop")
          if (itemprop != null && itemprop.equalsIgnoreCase("title")) {
            article.addCategory(elem.getText.toString)
          }
        }
  
        // keywords
        val segmenter = new JiebaSegmenter
        val word_list = segmenter.process(content, JiebaSegmenter.SegMode.INDEX).toList.map(token => token.word.getToken)
        val distinct_world_list = word_list
                        .filter{ x => x.length > 1 }
                        .filter{ x => ! TopicModel.is_stopword(x) }
                        .filter{ x => ! x.contains(".")} // ignore any keywords containing dot
                        .groupBy{x => x}
                        .map{ case (key, value) => (key, value.size)}
        article.keywords = distinct_world_list
      } catch {
        case e : java.io.FileNotFoundException => 
          debug(e)
          article.valid = false
        case e : java.net.UnknownHostException =>
          debug(e)
          article.valid = false
        case e : Throwable =>
          error("Caught an unexpected exception: " + e)
      }

	    return article
  	}
}