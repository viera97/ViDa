package com.vida.feature.categorymanagement.di

import com.vida.domain.repository.CategoryRepository
import com.vida.domain.usecase.category.AddCategory
import com.vida.domain.usecase.category.DeleteCategory
import com.vida.domain.usecase.category.GetCategory
import com.vida.domain.usecase.category.UpdateCategory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt module providing category-specific use cases for ViewModels.
 *
 * NOTE: [com.vida.domain.usecase.category.ListCategories] is NOT provided here —
 * it is already provided by [com.vida.feature.expense.di.ExpenseModule] in the
 * same [ViewModelComponent]. Duplicating it would cause a Dagger binding error.
 */
@Module
@InstallIn(ViewModelComponent::class)
object CategoryModule {

    @Provides
    fun provideAddCategory(repo: CategoryRepository): AddCategory =
        AddCategory(repo)

    @Provides
    fun provideUpdateCategory(repo: CategoryRepository): UpdateCategory =
        UpdateCategory(repo)

    @Provides
    fun provideDeleteCategory(repo: CategoryRepository): DeleteCategory =
        DeleteCategory(repo)

    @Provides
    fun provideGetCategory(repo: CategoryRepository): GetCategory =
        GetCategory(repo)
}
