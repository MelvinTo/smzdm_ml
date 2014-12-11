package me.hatu.smzdm

import com.sun.syndication.io._
import com.sun.syndication.feed.synd._
import scala.collection.JavaConversions._
import java.net.URL
import com.mongodb.casbah.Imports._
import com.huaban.analysis.jieba.JiebaSegmenter
import grizzled.slf4j.Logging
import org.htmlcleaner.HtmlCleaner

object FeedParser extends Logging {

  val mongodb_server : String = "192.168.59.103"
  val mongodb_port : Integer = 27017
  private val USER_AGENT : String = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:23.0) Gecko/20100101 Firefox/23.0";
  var stopwords : List[String] = List[String]()

  def load_stopwords {
    debug("Load stopwords...")
    stopwords = scala.io.Source.fromFile("stop.lst").mkString.split("\n").toList
  }

  def is_stopword(word : String) : Boolean = {
    return stopwords.contains(word)
  }

  def load_article(title: String, link: String) : Article = {
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
                    .filter{ x => ! is_stopword(x) }
                    .filter{ x => ! x.contains(".")} // ignore any keywords containing dot
                    .groupBy{x => x}
                    .map{ case (key, value) => (key, value.size)}
    article.keywords = distinct_world_list

    return article
  }

  def store_article(article: Article) {
    val mongoClient = MongoClient(mongodb_server, mongodb_port)
    val db = mongoClient("smzdm")
    val coll = db("articles")

    val obj = MongoDBObject(
        "title" -> article.title,
        "link"  -> article.link,
        "content" -> article.content,
        "category" ->  article.categories,
        "keywords" -> article.keywords
    )

    try {
      coll.insert(obj)
    } catch {
      case e : com.mongodb.MongoException.DuplicateKey => debug("entry %s already exists" format (article.link))
      case other : Throwable => error("got error: " + other)
    }
  }

  def article_exists(link: String) {
    val q = MongoDBObject("link" -> link)
    val cursor = mongoColl.find(q)
  }

  def process_article(article: String) : Unit = {
    val segmenter = new JiebaSegmenter
    val word_list = segmenter.process(article, JiebaSegmenter.SegMode.INDEX).toList.map(token => token.word.getToken)
    val distinct_world_list = word_list
                    .filter{ x => x.length > 2 }
                    .filter{ x => ! is_stopword(x) }
                    .groupBy{x => x}
                    .map{ case (key, value) => (key, value.size)}
    println(distinct_world_list.toList sortBy (- _._2 ) foreach {
      case (key, value) => print("[" + key + " = " + value + "]")
    })
    info("--------------")
  }

  def store_feed(coll : MongoCollection, entry: SyndEntryImpl): Unit = {
    val link = entry.getLink
    val link_trimmed = link.replaceAll("""(?m)\s+$""", "").replaceAll("""(?m)^\s+""","")
    val obj = MongoDBObject(
        "title" -> entry.getTitle,
        "link"  -> link_trimmed,
        "content" -> entry.getDescription.getValue
    )

    try {
      coll.insert(obj)
    } catch {
      case e : com.mongodb.MongoException.DuplicateKey => println("entry %s already exists" format (link_trimmed))
    }

  }

  def main(args: Array[String]): Unit = {
    val a = load_article("Pedigree 宝路 成犬全面营养牛肉狗粮 7.5kg", "http://www.smzdm.com/youhui/625415")
    store_article(a)

//     try {
//       // logger.info("xxx")
//       // load stopwords
//       load_stopwords

//       val mongoClient = MongoClient(mongodb_server, mongodb_port)
//       val db = mongoClient("smzdm")
//       val coll = db("rss_feeds")

//       val sfi = new SyndFeedInput()

//       val urls = List("http://feed.smzdm.com")
// //            val urls = List("http://feed.smzdm.com", "http://haitao.smzdm.com/feed", "http://jy.smzdm.com/feed", "http://show.smzdm.com/feed", "http://fx.smzdm.com/feed", "http://news.smzdm.com/feed")
//       urls.foreach(url => {
//         var conn = new URL(url).openConnection()
//         conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB;     rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 (.NET CLR 3.5.30729)")
//         val feed = sfi.build(new XmlReader(conn))

//         val entries = feed.getEntries()

//         entries.toList.foreach { 
// //          case entry : SyndEntryImpl => store_feed(coll, entry)
//           case entry : SyndEntryImpl => process_article(entry.getDescription.getValue)
//           case entry => throw new Exception("Expecting SyndEntryImpl, got a " + entry.getClass.getName)
//         }

//         println(feed.getTitle())
//         println(entries.size())
//       })
//     } catch {
//       case e : Throwable => throw new RuntimeException(e)
//     }
    
  }
}
