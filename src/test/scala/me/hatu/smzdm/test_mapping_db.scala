package me.hatu.smzdm

import org.scalatest.FunSuite
import grizzled.slf4j.Logging
import com.typesafe.config._
import java.io.File
import scala.collection.JavaConversions._

class TestMappingDB extends FunSuite with Logging {
  test("Should be able to create and store a mapping") {
    MappingDBManager.clean_all_mappings

    val mapping = new ArticleTopicsMapping("1")
    mapping.topics = Map("1" -> 1, "2" -> 2)
    MappingDBManager.store_mapping(mapping)
    assert(MappingDBManager.exists("1", "1") == true)
    assert(MappingDBManager.exists("1", "2") == true)
    assert(MappingDBManager.exists("1", "3") == false)
    assert(MappingDBManager.exists("1", "1", 1) == true)
    assert(MappingDBManager.exists("1", "2", 2) == true)
    assert(MappingDBManager.exists("1", "2", 3) == false)
    assert(MappingDBManager.exists("1", "3", 3) == false)
  }

  test("Should be able to create and store many mappings") {
    MappingDBManager.clean_all_mappings
    val mapping = new ArticleTopicsMapping("1")
    mapping.topics = Map("1" -> 1, "2" -> 2)
    val mapping2 = new ArticleTopicsMapping("2")
    mapping2.topics = Map("a1" -> 11, "a2" -> 22)

    MappingDBManager.store_all_mappings(List(mapping, mapping2))
    assert(MappingDBManager.exists("1", "1") == true)
    assert(MappingDBManager.exists("1", "1", 1) == true)
    assert(MappingDBManager.exists("1", "2") == true)
    assert(MappingDBManager.exists("1", "2", 2) == true)
    assert(MappingDBManager.exists("2", "a1") == true)
    assert(MappingDBManager.exists("2", "a2") == true)
    assert(MappingDBManager.exists("2", "a3") == false)
    assert(MappingDBManager.exists("2", "a2", 22) == true)
    assert(MappingDBManager.exists("2", "a2", 33) == false)

  }
}