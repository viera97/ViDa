// TODO: Re-enable once Hilt test infrastructure is in place (requires test AndroidManifest.xml
//       with HiltTestApplication, or proper Robolectric Hilt integration in :app module).
//       Same constraint as PR #1's HiltGraphSmokeTest and PR #2's HiltTransactionGraphTest.
// @HiltAndroidTest
// @RunWith(RobolectricTestRunner::class)
// @Config(sdk = [34])
// class HiltMovementGraphTest {
//     @get:Rule val hiltRule = HiltAndroidRule(this)
//
//     @Inject lateinit var database: AppDatabase
//     @Inject lateinit var transferDao: TransferDao
//     @Inject lateinit var recurringExpenseDao: RecurringExpenseDao
//     @Inject lateinit var balanceDao: BalanceDao
//     @Inject lateinit var transferOrchestrator: TransferOrchestrator
//     @Inject lateinit var appDatabaseCallback: AppDatabaseCallback
//     @Inject lateinit var transferRepository: TransferRepository
//     @Inject lateinit var recurringExpenseRepository: RecurringExpenseRepository
//
//     @Test
//     fun `movement graph resolves all new bindings`() {
//         hiltRule.inject()
//         assertNotNull("AppDatabase must be provided", database)
//         assertNotNull("TransferDao must be provided", transferDao)
//         assertNotNull("RecurringExpenseDao must be provided", recurringExpenseDao)
//         assertNotNull("BalanceDao must be provided", balanceDao)
//         assertNotNull("TransferOrchestrator must be provided", transferOrchestrator)
//         assertNotNull("AppDatabaseCallback must be provided", appDatabaseCallback)
//         assertNotNull("TransferRepository must be provided", transferRepository)
//         assertNotNull("RecurringExpenseRepository must be provided", recurringExpenseRepository)
//     }
// }