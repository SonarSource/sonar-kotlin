package org.sonarsource.kotlin.buildsrc.tasks

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.result.Result

private const val GITHUB_AUTH_TOKEN_ENV_NAME = "GH_API_TOKEN"
private val GH_TOKEN by lazy {
    System.getenv(GITHUB_AUTH_TOKEN_ENV_NAME).let {
        if (it.isNullOrBlank()) null else it
    }
}

private const val PR_QUERY_STRING = "is:pr is:open repo:SonarSource/rspec label:kotlin"

internal object GitHubApi {
    fun getBranchCandidatesForRule(ruleKey: String) =
        preparePrSearchQuery(ruleKey).getResponseObject<PrSearchResults>().let { (response, result) ->
            val (searchResults, error) = result
            error?.let {
                System.err.println("Request: ${response.url}")
                System.err.println("Response: ${response.data.decodeToString()}")
                throw it
            }
            searchResults?.items ?: throw IllegalStateException("Got null response while searching PRs for rule key $ruleKey.")
        }.map { searchResult ->
            preparePrDetailsQuery(searchResult.number).getResponseObject<Pr>().let { (response, result) ->
                val (pr, error) = result
                error?.let {
                    System.err.println("Request: ${response.url}")
                    System.err.println("Response: ${response.data.decodeToString()}")
                    throw it
                }
                if (pr == null) throw IllegalStateException("Got null response while fetching PR #${searchResult.number} details.")
                pr
            }
        }

    private fun preparePrSearchQuery(ruleKey: String) =
        Endpoints.PR_SEARCH.httpGet(listOf("q" to "$PR_QUERY_STRING $ruleKey"))
            .prepRequest()

    private fun preparePrDetailsQuery(prId: Int) =
        "${Endpoints.PR_DETAILS}/$prId".httpGet()
            .prepRequest()

    private fun Request.prepRequest() =
        (GH_TOKEN?.let { token ->
            header("Authorization", "Bearer $token")
        } ?: this)
            .header("Accept", "application/vnd.github+json")
}

private inline fun <reified T : Any> Request.getResponseObject(): Pair<Response, Result<T, FuelError>> {
    var retResult: Result<T, FuelError>? = null
    var retResponse: Response? = null
    responseObject<T> { _, response, result ->
        retResult = result
        retResponse = response
    }.join()
    return retResponse!! to retResult!!
}

private object Endpoints {
    const val BASE_URL = "https://api.github.com"
    const val PR_SEARCH = "$BASE_URL/search/issues"
    const val PR_DETAILS = "$BASE_URL/repos/SonarSource/rspec/pulls"
}

data class PrSearchResults(val items: List<PrSearchResult>)
data class PrSearchResult(val number: Int)
data class Pr(val head: Head, val title: String, val html_url: String, val number: Int)
data class Head(val ref: String)
