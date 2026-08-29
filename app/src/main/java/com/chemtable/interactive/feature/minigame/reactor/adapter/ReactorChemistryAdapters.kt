package com.chemtable.interactive.feature.minigame.reactor.adapter

import com.chemtable.interactive.feature.minigame.engine.FormulaMassResolver
import com.chemtable.interactive.feature.minigame.engine.RecipeBook
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorMassAuthority
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorProductSpecification
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorReactionCatalog
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorComposition

/** Delegates Reactor multiset lookups to the one canonical Classic gameplay recipe authority. */
class ClassicRecipeBookReactorAdapter(
    private val recipeBook: RecipeBook = RecipeBook(),
) : ReactorReactionCatalog {
    override fun findProduct(composition: ReactorComposition): ReactorProductSpecification? =
        recipeBook.match(composition.counts)?.let { recipe ->
            ReactorProductSpecification(
                formula = recipe.productFormula,
                displayName = recipe.displayKo,
                composition = ReactorComposition.of(recipe.inputs),
            )
        }
}

/** Uses the existing formula-mass port; the Reactor engine only sees its own pure mass interface. */
class FormulaMassResolverReactorMassAdapter(
    private val resolver: FormulaMassResolver,
) : ReactorMassAuthority {
    override fun molarMassOf(specification: ReactorProductSpecification): Double =
        resolver.molarMassOf(specification.formula)
}
