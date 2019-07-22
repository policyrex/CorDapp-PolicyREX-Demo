package com.policyrex.flow

import co.paralleluniverse.fibers.Suspendable
import com.policyrex.contract.UserContract
import com.policyrex.state.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

object UserCreateNewFlow {
    @InitiatingFlow
    @StartableByRPC
    class UserInitiator(
            val policyREXNode: Party,
            val firt_name: String,
            val last_name: String,
            val user_name: String,
            val id_card: String,
            val email: String,
            val phone: String,
            val address: String,
            val value: Int
    ) : FlowLogic<SignedTransaction>()
    {

        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object USER_CREATE_NEW : ProgressTracker.Step("User sends information to the PolicyREX")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object COMPANY_RESPONSE : ProgressTracker.Step("Insurance Company responds to Applicant")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    USER_CREATE_NEW,
                    VERIFYING_TRANSACTION,
                    COMPANY_RESPONSE,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): SignedTransaction {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0];

            // Stage 1.
            progressTracker.currentStep = USER_CREATE_NEW
            // Generate an unsigned transaction.
            val status:Int = 0

            val userState = UserState(serviceHub.myInfo.legalIdentities.first(),policyREXNode, firt_name, last_name, user_name, id_card, email, phone, address, status)
            val initiateClaimCommand = Command(UserContract.Commands.UserApplication(),userState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(userState, UserContract.USER_CONTRACT_ID)
                    .addCommand(initiateClaimCommand)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 4.
            progressTracker.currentStep = GATHERING_SIGS
            // Send the state to the Insurance Company, and receive it back with their signature.
            val otherPartyFlow = initiateFlow(policyREXNode)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow), GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    @InitiatedBy(UserInitiator::class)
    class Acceptor(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be a Claim Application transaction." using (output is UserState)
                    val out = output as UserState
                    "UserName should be not null" using (out.user_name != null)
                }
            }
            return subFlow(signTransactionFlow)
        }
    }
}