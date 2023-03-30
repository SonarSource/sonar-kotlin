/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package my.org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.returnType
import org.sonarsource.kotlin.api.secondaryOf
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val GETTER_PREFIX = Regex("""^(get|is)\p{javaUpperCase}""")
private val SETTER_PREFIX = Regex("""^set\p{javaUpperCase}""")

@Rule(key = "S6512")
class PropertyGetterAndSetterUsageCheck : AbstractCheck() {

    @RuleProperty(
        key = "checkPublicClasses",
        description = "Check getters and setters on public classes",
        defaultValue = "false"
    )
    var checkPublicClasses = false

    override fun visitClass(klass: KtClass, ctx: KotlinFileContext) {
        if (klass.isPublic && !checkPublicClasses) {
            // avoid asking to change for property get() and set(value) to not break a public API
            return
        }
        val classBody = klass.body ?: return
        val javaAccessors = classBody.functions
            .filter { isGetterOrSetter(it.valueParameters.size, it.name ?: "") }
            .groupBy { it.name ?: "" }

        if (javaAccessors.isEmpty()) {
            return
        }
        val bindingCtx = ctx.bindingContext
        classBody.properties
            .filter { isPrivateWithoutGetterAndSetter(it) }
            .forEach { checkProperty(it, javaAccessors, ctx, bindingCtx) }
    }

    private fun checkProperty(
        prop: KtProperty, javaAccessors: Map<String, List<KtNamedFunction>>, ctx: KotlinFileContext, bindingCtx: BindingContext
    ) {
        prop.nameIdentifier?.let { propIdentifier ->
            prop.typeReference?.let { bindingCtx.get(BindingContext.TYPE, it) }?.let { propType ->
                val getterIdentifier = findJavaStyleGetterFuncIdentifier(propIdentifier.text, propType, javaAccessors, bindingCtx)
                val setterIdentifier = findJavaStyleSetterFuncIdentifier(propIdentifier.text, propType, javaAccessors, bindingCtx)
                if (getterIdentifier != null) {
                    val secondaries = when (setterIdentifier) {
                        null -> listOf(ctx.secondaryOf(propIdentifier, """Property "${propIdentifier.text}""""))
                        else -> listOf(
                            ctx.secondaryOf(propIdentifier, """Property "${propIdentifier.text}""""),
                            ctx.secondaryOf(setterIdentifier, """Setter to convert to a "set(value)"""")
                        )
                    }
                    ctx.reportIssue(
                        getterIdentifier,
                        """Convert this getter to a "get()" on the property "${propIdentifier.text}".""",
                        secondaries
                    )
                } else if (setterIdentifier != null) {
                    val secondaries = listOf(ctx.secondaryOf(propIdentifier, """Property "${propIdentifier.text}""""))
                    ctx.reportIssue(
                        setterIdentifier,
                        """Convert this setter to a "set(value)" on the property "${propIdentifier.text}".""",
                        secondaries
                    )
                }
            }
        }
    }
}

private fun isPrivateWithoutGetterAndSetter(prop: KtProperty): Boolean = prop.isPrivate() && prop.accessors.isEmpty()

private fun isGetterOrSetter(parameterCount: Int, functionName: String) =
    (parameterCount == 0 && GETTER_PREFIX.find(functionName) != null) ||
        (parameterCount == 1 && SETTER_PREFIX.find(functionName) != null)

private fun findJavaStyleGetterFuncIdentifier(
    propName: String, propType: KotlinType, javaAccessors: Map<String, List<KtNamedFunction>>, bindingCtx: BindingContext
): PsiElement? {
    val capitalizedName = capitalize(propName)
    val functionsPrefixedByIs = if (propType.matches("kotlin.Boolean")) {
        javaAccessors.getOrElse("is${capitalizedName}") { emptyList() }
    } else {
        emptyList()
    }
    return (javaAccessors.getOrElse("get${capitalizedName}") { emptyList() } + functionsPrefixedByIs)
        .filter { it.returnType(bindingCtx) == propType }
        .unambiguousIdentifier()
}

private fun parameterMatchesType(parameter: KtParameter, type: KotlinType, bindingCtx: BindingContext): Boolean {
    return !parameter.isVarArg && type == (parameter.typeReference?.let { bindingCtx.get(BindingContext.TYPE, it) })
}

private fun findJavaStyleSetterFuncIdentifier(
    propName: String, propType: KotlinType, javaAccessors: Map<String, List<KtNamedFunction>>, bindingCtx: BindingContext
): PsiElement? =
    javaAccessors.getOrElse("set${capitalize(propName)}") { emptyList() }
        .filter { it.returnType(bindingCtx)?.matches("kotlin.Unit") ?: false }
        // isGetterOrSetter ensures setters have: valueParameters.size == 1
        .filter { parameterMatchesType(it.valueParameters[0], propType, bindingCtx) }
        .unambiguousIdentifier()

private fun List<KtNamedFunction>.unambiguousIdentifier() =
    if (this.size == 1) this[0].nameIdentifier else null /* empty or ambiguous */

private fun capitalize(name: String): String = name.replaceFirstChar { it.uppercase() }

private fun KotlinType.matches(qualifiedTypeName: String) =
    when {
        isNullable() -> false
        else -> getJetTypeFqName(true) == qualifiedTypeName
    }
