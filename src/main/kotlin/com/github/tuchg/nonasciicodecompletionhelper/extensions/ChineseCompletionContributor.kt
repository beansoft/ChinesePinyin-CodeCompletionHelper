package com.github.tuchg.nonasciicodecompletionhelper.extensions

import com.github.tuchg.nonasciicodecompletionhelper.extensions.ChineseCompletionContributor.Companion.cache
import com.github.tuchg.nonasciicodecompletionhelper.model.ChineseLookupElement
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PlainPrefixMatcher
import pansong291.simplepinyin.Pinyin

// private val cache = HashMap<String, Boolean>()
/**
 * @author tuchg
 * @date 2020-8-1
 */
class ChineseCompletionContributor : CompletionContributor() {
//    private val maxValue = Int.MAX_VALUE - 200 //用于补全项排序

    companion object {
        @JvmStatic
        val cache = HashMap<String, Int>()

    }

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val resultSet = result
                .withPrefixMatcher(ChinesePrefixMatcher(result.prefixMatcher.prefix))
//                .withRelevanceSorter(CompletionSorter.defaultSorter(parameters, result.prefixMatcher).weigh(
//                        object : LookupElementWeigher("ChineseCompletionElementWeigher", false, true) {
//                            override fun weigh(element: LookupElement) = if (element is ChineseLookupElement) element.index else Int.MAX_VALUE
//                        }
//                ))
        resultSet.restartCompletionOnAnyPrefixChange()
        resultSet.addLookupAdvertisement("输入拼音,补全中文标识符")
        resultSet.runRemainingContributors(parameters) { r ->
            val element = r.lookupElement
            if (Pinyin.hasChinese(element.lookupString)) {
                var flag = 0
                // 从多音字列表提取命中次数最多的一个
                val closest = Pinyin.toPinyin(element.lookupString, Pinyin.FIRST_UP_CASE)?.let {
                    val prefix = r.prefixMatcher.prefix
                    if (it.size == 1) {
                        return@let if (countContainsSomeChar(it[0].toLowerCase(), prefix) >= prefix.length) {
                            flag += 10
                            it[0]
                        } else null
                    }
                    it.maxBy {
                        val count = countContainsSomeChar(it.toLowerCase(), prefix)
                        if (count >= prefix.length) {
                            flag++
                            count
                        } else -1
                    }
                }
                closest?.let {
                    if (flag > 0) {
                        // 追加补全列表
                        resultSet.addElement(ChineseLookupElement(-1, element.lookupString, it).copyOtherLookup(r.lookupElement))
                    }
                }
            } else
                resultSet.passResult(r)
        }
    }
}

class ChinesePrefixMatcher(prefix: String) : PlainPrefixMatcher(prefix) {

    override fun prefixMatches(name: String): Boolean {
        return if (Pinyin.hasChinese(name)) {
            for (s in Pinyin.toPinyin(name, Pinyin.LOW_CASE)) {
                if (countContainsSomeChar(s, prefix) >= prefix.length) {
                    return true
                }
            }
            return false
        } else super.prefixMatches(name)
    }

    override fun cloneWithPrefix(prefix: String) = if (prefix == this.prefix) this else ChinesePrefixMatcher(prefix)
}


/**
 * 计算字符串内包含需要的字符的数量
 * @param origin 文本串
 * @param needed 模式串
 */
private fun countContainsSomeChar(origin: String, needed: String): Int {
    val key = "$origin-$needed"
    cache[key]?.let {
        return it
    }
    val counted = mutableSetOf<Int>()
    var i = 0
    var j = 0
    while (i < needed.length) {
        val indexOf = origin.indexOf(needed[i], j++)
        if (indexOf != -1) {
            if (counted.add(indexOf)) {
                i++
            }
        } else {
            i++
        }
    }
    val size = counted.size
    cache[key] = size
    return size
}
