package me.hatu.smzdm

import grizzled.slf4j.Logging
import org.htmlcleaner.HtmlCleaner
import com.huaban.analysis.jieba.JiebaSegmenter
import java.net.URL
import scala.collection.JavaConversions._



object WebParser extends Logging {
	private val USER_AGENT : String = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:23.0) Gecko/20100101 Firefox/23.0";

	def parse_article(title: String, link: String) : Article = {
	    var article = new Article(title)
	    article.link = link

	    val cleaner = new HtmlCleaner
	    val props = cleaner.getProperties

	    val lConn = new URL(link).openConnection();
	    lConn.setRequestProperty("User-Agent", USER_AGENT)
	    lConn.connect();
	    val rootNode = cleaner.clean( lConn.getInputStream() )

	    // content 
	    val p_elements = rootNode.getElementsByName("p", true)
	    var content = ""
	    for (elem <- p_elements) {
	      val itemprop = elem.getAttributeByName("itemprop")
	      if (itemprop != null && itemprop.equalsIgnoreCase("description")) {
	        content += elem.getText.toString
	        debug(elem.getText.toString)
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

	    return article
  	}
}