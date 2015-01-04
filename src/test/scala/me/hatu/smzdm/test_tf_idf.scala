package me.hatu.smzdm

import org.scalatest.FunSuite
import grizzled.slf4j.Logging
import com.typesafe.config._
import java.io.File
import scala.collection.JavaConversions._

class TestTFIDF extends FunSuite with Logging {
  test("Should be able to load a document") {
    val doc = TopicModel.parse_document("title1", "this is a document content for title1")
    assert(doc.ws.categoryValues.size == 3)


  }

  test("Should be able to get df") {
    val doc = TopicModel.parse_document("title1", "this is a document content for title1")
    val df = TopicModel.get_df(doc)
    assert(df.size == 3)
    assert(df("title1") == 1)
    assert(df.contains("this") == false)

    val doc2 = TopicModel.parse_document("title2", "title2 is the only title2")
    val df2 = TopicModel.get_df(doc2)
    assert(df2.size == 1)
    assert(df2("title2") == 1)
  }

  test("Should be able to merge df") {
    val doc = TopicModel.parse_document("title1", "this is a document content for title1")
    val df = TopicModel.get_df(doc)

    val doc2 = TopicModel.parse_document("title2", "this is a document content for title2")
    val df2 = TopicModel.get_df(doc2)

    val merged = TopicModel.merge_dfs(List(df, df2))
    assert(merged.size == 4)
    assert(merged("title1") == 1)
    assert(merged("title2") == 1)
    assert(merged("document") == 2)
    assert(merged("content") != 3)
  }

  test("Should be able to get tf") {
    val doc = TopicModel.parse_document("this_is_title2", "title2 is the only title2")
    val tf = TopicModel.get_tf(doc)
    assert(tf.size == 1)
    assert(tf(0)._1 == "this_is_title2")
    assert(tf(0)._2 == "title2")
    assert(tf(0)._3 == 2)
  }

  test("Should be able to get tf-idf") {
    val doc1 = TopicModel.parse_document("title1", "this is a document content for title1")
    val df1 = TopicModel.get_df(doc1)

    val doc2 = TopicModel.parse_document("title2", "this is a document content for title2")
    val df2 = TopicModel.get_df(doc2)

    val doc3 = TopicModel.parse_document("this_is_title2", "title2 is the only title2")
    val df3 = TopicModel.get_df(doc3)

    val doc4 = TopicModel.parse_document("this_is_title3", "title2 is my title2")
    val df4 = TopicModel.get_df(doc4)

    val doc5 = TopicModel.parse_document("this_is_title4", "title2 is my title2, title2 is my title2")
    val df5 = TopicModel.get_df(doc5)

    val merged_df = TopicModel.merge_dfs(List(df1, df2, df3, df4, df5))

    val tf1 = TopicModel.get_tf(doc1)
    val tf_idf1 = TopicModel.get_tf_idf(merged_df, tf1, 5)
    debug(tf_idf1)

    val tf2 = TopicModel.get_tf(doc2)
    val tf_idf2 = TopicModel.get_tf_idf(merged_df, tf2, 5)
    debug(tf_idf2)
  }

  test("Should be able to run get_all_tfs") {
    val doc1 = TopicModel.parse_document("title1", "this is a document content for title1")
    val df1 = TopicModel.get_df(doc1)

    val doc2 = TopicModel.parse_document("title2", "this is a document content for title2")
    val df2 = TopicModel.get_df(doc2)

    val doc3 = TopicModel.parse_document("this_is_title2", "title2 is the only title2")
    val df3 = TopicModel.get_df(doc3)

    val doc4 = TopicModel.parse_document("this_is_title3", "title2 is my title2")
    val df4 = TopicModel.get_df(doc4)

    val doc5 = TopicModel.parse_document("this_is_title4", "title2 is my title2, title2 is my title2")
    val df5 = TopicModel.get_df(doc5)

    val merged_df = TopicModel.merge_dfs(List(df1, df2, df3, df4, df5))

    val tfs = TopicModel.get_all_tfs(List(doc1, doc2, doc3, doc4, doc5))
    assert(tfs.contains(("title1","title1",1)) == true)
    assert(tfs.contains((("this_is_title4","title2",4))) == true)

    val tf_idfs = TopicModel.get_tf_idf(merged_df, tfs, 5)
    debug(tf_idfs)
  }

  test("A larger test on tf-idf") {
    val list = TestTraining.loadArticles.take(1400)
    val docs = list.map(x => TopicModel.parse_document(x.title, x.content))
    val doc_cnt = docs.size
    val dfs = TopicModel.get_all_dfs(docs)
    // dfs.take(100).foreach(x => debug(x))

    val tfs = TopicModel.get_all_tfs(docs)
    // tfs.take(100).foreach(x => debug(x))

    val tf_idfs = TopicModel.get_tf_idf(dfs, tfs, doc_cnt)
    // val small_tf_idfs = tf_idfs.sortBy(x => x._3).take(100)
    val grouped_tf_idfs = tf_idfs .groupBy(x => x._2)
                                  .mapValues(y => y.map(z => z._3).sum / y.size)
                                  .toList.sortBy ( _._2 ).take(500)
    grouped_tf_idfs.foreach(x => debug(x))
  }
}