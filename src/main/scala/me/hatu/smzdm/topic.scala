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

object TopicModel extends Logging {

  var stopwords : List[String] = List[String]()
  var stopwords_loaded: Boolean = false

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

  def train(articles: List[Article]) {
    object WordSeqDomain extends CategoricalSeqDomain[String]
    implicit val random = new scala.util.Random(0)
    val model = DirectedModel()
    // number of topics: 20
    // alpha: 0.1
    // beta: 0.01
    // optimizeBurnIn: 100
    val lda = new LDA(WordSeqDomain, 40, 0.1, 0.01, 100)(model,random)
    val mySegmenter = new JiebaWordSegmenter
    articles.foreach( x => {
      val doc = Document.fromString(WordSeqDomain, x.title, x.content, segmenter = mySegmenter)
      lda.addDocument(doc, random)
    })

    debug("Read "+lda.documents.size+" documents, "+WordSeqDomain.elementDomain.size+" word types, "+lda.documents.map(_.ws.length).sum+" word tokens.")


    // number of iterations: 100
    lda.inferTopics(100)

    // debug(lda.topicsSummary(20))
    // debug(lda.topicsWordsAndPhrasesSummary(20, 20))

  }
}