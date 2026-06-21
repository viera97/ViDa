// TODO: Re-enable once Hilt test infrastructure is in place (requires test AndroidManifest.xml
//       with HiltTestApplication, or proper Robolectric Hilt integration in :app module).
// @HiltAndroidTest
// @RunWith(RobolectricTestRunner::class)
// @Config(sdk = [34])
// class HiltGraphSmokeTest {
//     @get:Rule val hiltRule = HiltAndroidRule(this)
//
//     @Inject lateinit var database: AppDatabase
//     @Inject lateinit var cardDao: CardDao
//     @Inject lateinit var stashDao: StashDao
//     @Inject lateinit var walletDao: WalletDao
//     @Inject lateinit var cardRepository: CardRepository
//     @Inject lateinit var stashRepository: StashRepository
//     @Inject lateinit var walletRepository: WalletRepository
//     @Inject lateinit var passphraseProvider: PassphraseProvider
//
//     @Test
//     fun `database graph resolves all bindings`() {
//         hiltRule.inject()
//         assertNotNull("AppDatabase must be provided", database)
//         assertNotNull("CardDao must be provided", cardDao)
//         assertNotNull("StashDao must be provided", stashDao)
//         assertNotNull("WalletDao must be provided", walletDao)
//         assertNotNull("CardRepository must be provided", cardRepository)
//         assertNotNull("StashRepository must be provided", stashRepository)
//         assertNotNull("WalletRepository must be provided", walletRepository)
//         assertNotNull("PassphraseProvider must be provided", passphraseProvider)
//     }
// }