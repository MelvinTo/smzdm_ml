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

  def get_df(doc: Document) : Map[String, Int] = {
    return doc.ws.categoryValues.toList.distinct.map(x => (x,1)).toMap
  }

  def merge_dfs(dfs: List[Map[String, Int]]): Map[String, Int] = {
    return dfs.map(x => x.toList).flatten
              .groupBy(x => x._1)
              .mapValues(x => x.map(y => y._2).sum)
  }

  def get_all_dfs(docs: List[Document]) : Map[String, Int] = {
    return merge_dfs(docs.map(get_df))
  }

  def get_tf(doc: Document) : List[(String, String, Int)] = {
    return doc.ws.categoryValues.toList.groupBy(x => x)
              .mapValues(x => x.length).toList
              .map(x => (doc.name, x._1, x._2))
  }

  def get_all_tfs(docs: List[Document]) : List[(String, String, Int)] = {
    return docs.map(get_tf).flatten
  }

  def get_tf_idf(df: Map[String, Int], tf: List[(String, String, Int)], doc_count: Int): List[(String, String, Double)] = {
    return tf.map( x => {
      val doc_name = x._1
      val term = x._2
      val term_df = df(term)
      val term_tf = x._3
      val idf = scala.math.log((term_df.toDouble)/doc_count)
      val weight = term_df*idf
      (doc_name, term, weight)
    })
  }

  def parse_document(title: String, content: String) : Document = {
    val mySegmenter = new JiebaWordSegmenter
    val doc = Document.fromString(WordSeqDomain, title, content, segmenter = mySegmenter)
    return doc
  }

  def train(articles: List[Article]) : LDA = {
    implicit val random = new scala.util.Random(0)
    val model = DirectedModel()
    // number of topics: 100
    // alpha: 0.1
    // beta: 0.01
    // optimizeBurnIn: 100
    val lda = new LDA(WordSeqDomain, 100, 0.1, 0.01, 100)(model,random)
    articles.zipWithIndex.foreach{ case(x, i) => 
      val doc = parse_document(x.title + " " + i, x.content)
      if(doc.ws.length > 0) {
        try {
          lda.addDocument(doc, random)
        } catch {
          case e : java.lang.IllegalArgumentException =>
            error(f"illegal doc: $x%s due to $e%s")
        }
      }
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