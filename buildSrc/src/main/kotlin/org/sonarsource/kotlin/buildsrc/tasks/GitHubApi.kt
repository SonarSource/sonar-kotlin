package org.sonarsource.kotlin.buildsrc.tasks

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.awaitResult
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
        preparePrSearchQuery(ruleKey).getResponseObject<PrSearchResults>().let { (searchResults, error) ->
            error?.logAndThrow()
            searchResults?.items ?: throw IllegalStateException("Got null response while searching PRs for rule key $ruleKey.")
        }.map { searchResult ->
            // For every PR, we extract the branch name to suggest as alternative branch as metadata source.
            preparePrDetailsQuery(searchResult.number).getResponseObject<Pr>().let { (pr, error) ->
                error?.logAndThrow()
                pr ?: throw IllegalStateException("Got null response while fetching PR #${searchResult.number} details.")
            }
        }

    /**
     * To find the PRs that contain the rule key in their title
     */
    private fun preparePrSearchQuery(ruleKey: String) =
        Endpoints.PR_SEARCH.httpGet(listOf("q" to "$PR_QUERY_STRING $ruleKey"))
            .prepRequest()

    /**
     * To get details about a particular PR
     */
    private fun preparePrDetailsQuery(prId: Int) =
        "${Endpoints.PR_DETAILS}/$prId".httpGet()
            .prepRequest()

    /**
     * Add common headers to requests
     */
    private fun Request.prepRequest() =
        (GH_TOKEN?.let { token ->
            header("Authorization", "Bearer $token")
        } ?: this)
            .header("Accept", "application/vnd.github+json")
}

private fun FuelError.logAndThrow() {
    System.err.println("Request: ${response.url}")
    System.err.println("Response: ${response.data.decodeToString()}")
    throw this
}

private inline fun <reified T : Any> Request.getResponseObject(): Result<T, FuelError> =
    responseObject<T>().third

private object Endpoints {
    const val BASE_URL = "https://api.github.com"
    const val PR_SEARCH = "$BASE_URL/search/issues"
    const val PR_DETAILS = "$BASE_URL/repos/SonarSource/rspec/pulls"
}

data class PrSearchResults(val items: List<PrSearchResult>)
data class PrSearchResult(val number: Int)
data class Pr(val head: Head, val title: String, val html_url: String, val number: Int)
data class Head(val ref: String)
