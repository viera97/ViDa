// TODO: Re-enable once Hilt test infrastructure is in place (requires test AndroidManifest.xml
//       with HiltTestApplication, or proper Robolectric Hilt integration in :app module).
//       Same constraint as PR #1's HiltGraphSmokeTest.
// @HiltAndroidTest
// @RunWith(RobolectricTestRunner::class)
// @Config(sdk = [34])
// class HiltTransactionGraphTest {
//     @get:Rule val hiltRule = HiltAndroidRule(this)
//
//     @Inject lateinit var database: AppDatabase
//     @Inject lateinit var categoryDao: CategoryDao
//     @Inject lateinit var expenseDao: ExpenseDao
//     @Inject lateinit var refundDao: RefundDao
//     @Inject lateinit var currencyRateDao: CurrencyRateDao
//     @Inject lateinit var categoryRepository: CategoryRepository
//     @Inject lateinit var expenseRepository: ExpenseRepository
//     @Inject lateinit var refundRepository: RefundRepository
//     @Inject lateinit var currencyRateRepository: CurrencyRateRepository
//
//     @Test
//     fun `transaction graph resolves all new bindings`() {
//         hiltRule.inject()
//         assertNotNull("AppDatabase must be provided", database)
//         assertNotNull("CategoryDao must be provided", categoryDao)
//         assertNotNull("ExpenseDao must be provided", expenseDao)
//         assertNotNull("RefundDao must be provided", refundDao)
//         assertNotNull("CurrencyRateDao must be provided", currencyRateDao)
//         assertNotNull("CategoryRepository must be provided", categoryRepository)
//         assertNotNull("ExpenseRepository must be provided", expenseRepository)
//         assertNotNull("RefundRepository must be provided", refundRepository)
//         assertNotNull("CurrencyRateRepository must be provided", currencyRateRepository)
//     }
// }
