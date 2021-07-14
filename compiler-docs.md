# The Kotlin Compiler API: Techniques, Tips and Insights

## Function Calls

- Resolving function calls: there is the extension function `org.jetbrains.kotlin.resolve.calls.callUtil#getResolvedCall` defined on `KtElement?`. You need to pass it the binding context.
- Finding arguments by index `i`
    - <b>Attention!</b> Getting argument `i` from the `KtCallExpression` will not necessarily get you the value you are expecting and may even fail with an `IndexOutOfBoundsException`! `KtCallExpression`s are not resolved, hence if you get argument `i` from it, it is really the `i`th argument in the argument list, not the argument corresponding to the `i`th parameter (which is more likely what you want). The usage of named arguments and default parameter values can cause the order and length of the argument list to be unpredictable.
    - Instead, retrieve the argument value from the resolved call: `ResolvedCall#valueArgumentsByIndex`. This will give you a list ordered by parameter order and filled up with placeholders where the default parameter values are being used. Arguments to `vararg` parameters are also handled.
        - Note: <b>call this method once</b> and store the result if you need to access it multiple times. The method performs the argument resolution every time it is called.
