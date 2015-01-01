package me.hatu.smzdm

import cc.factorie.app.topics.lda.LDA
import cc.factorie.app.topics.lda.Document
import grizzled.slf4j.Logging
import cc.factorie.variable._
import cc.factorie.directed.DirectedModel
import cc.factorie.app.strings.StringSegmentIterator
import cc.factorie.app.strings.StringSegmenter
import com.huaban.analysis.jieba.JiebaSegmenter
import scala.collection.JavaConversions._
import java.io.File
import com.typesafe.config._

class JiebaWordSegmenter extends StringSegmenter with Logging {
  def apply(s: String) : StringSegmentIterator = new StringSegmentIterator {
    val segmenter = new JiebaSegmenter
    val word_list = segmenter.process(s, JiebaSegmenter.SegMode.INDEX).toList.map(token => token.word.getToken)
    val distinct_world_list = word_list
                                .filter{ x => x.length > 1 }
                                .filter{ x => ! TopicModel.is_stopword(x) }
                                .filter{ x => ! x.contains(".")} // ignore any keywords containing dot

    // debug(distinct_world_list)
    val i = distinct_world_list.iterator
    def hasNext = i.hasNext
    def next() = i.next()
    def start = -1
    def end = -1
  }
}

object WordSeqDomain extends CategoricalSeqDomain[String]

class ArticleTopicsMapping(article_idc: String) {
  var article_id: String = article_idc
  var topics: Map[String, Int] = Map()

  override def toString : String = {
    return f"$article_id%s - $topics%s"
  }
}

object TopicModel extends Logging {

  var stopwords : List[String] = List[String]()
  var stopwords_loaded: Boolean = false
  val top : Int = 5

  def load_stopwords {
    debug("Load stopwords...")
    val fileString = getClass.getClassLoader.getResource("stopwords.json").toURI
    val jsonFile = new File(fileString)
    val config = ConfigFactory.parseFile(jsonFile)
    stopwords = config.getStringList("stopwords").toList
    stopwords_loaded = true
  }
  
  def is_stopword(word : String) : Boolean = {
    if (! stopwords_loaded) {
      load_stopwords
    }
    return stopwords.contains(word)
  }

  def predict(lda: LDA, article: Article) : Document = {
    implicit val random = new scala.util.Random(0)
    val model = DirectedModel()

    val mySegmenter = new JiebaWordSegmenter
    val doc = Document.fromString(WordSeqDomain, article.title, article.content, segmenter = mySegmenter)
    lda.inferDocumentTheta(doc)
    debug(doc.ws.categoryValues.take(10).mkString(" "))
    debug(doc.theta)
    doc.thetaArray.zipWithIndex.sortWith(_._1 > _._1).take(5).foreach { 
        case (x, i) =>
          debug(i + ":" + x)
    }
    return doc
  }

  def get_mapping(doc: Document) : ArticleTopicsMapping = {
    val topicsMap = doc.thetaArray.zipWithIndex.map( x => (x._2.toString -> x._1.toInt)).toMap
    val mapping = new ArticleTopicsMapping(doc.name)
    mapping.topics = topicsMap
    // debug(mapping)
    return mapping
  }

  def get_top_topics(mapping : ArticleTopicsMapping) : List[(String, Int)] = {
    mapping.topics.toList.sortBy(_._2).reverse.take(top)
  }

  def get_all_mappings(lda: LDA) : List[ArticleTopicsMapping] = {
    return lda.documents.toList.map(x => get_mapping(x.asInstanceOf[Document]))
  }

  def train(articles: List[Article]) : LDA = {
    implicit val random = new scala.util.Random(0)
    val model = DirectedModel()
    // number of topics: 100
    // alpha: 0.1
    // beta: 0.01
    // optimizeBurnIn: 100
    val lda = new LDA(WordSeqDomain, 100, 0.1, 0.01, 100)(model,random)
    val mySegmenter = new JiebaWordSegmenter
    articles.zipWithIndex.foreach{ case(x, i) => 
      val doc = Document.fromString(WordSeqDomain, x.title + " " + i, x.content, segmenter = mySegmenter)
      lda.addDocument(doc, random)
    }

    debug("Read "+lda.documents.size+" documents, "+WordSeqDomain.elementDomain.size+" word types, "+lda.documents.map(_.ws.length).sum+" word tokens.")


    // number of iterations: 100
    lda.inferTopics(100)

    // get mappings
    val mappings = get_all_mappings(lda)
    MappingDBManager.clean_all_mappings
    MappingDBManager.store_all_mappings(mappings)

    debug("xxxxxxxxxxxxxx")
    debug(lda.topicsSummary(20))
    debug("xxxxxxxxxxxxxx")
    // debug(lda.topicsWordsAndPhrasesSummary(20, 20))
    // debug("xxxxxxxxxxxxxx")

    return lda
  }
}