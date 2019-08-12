package com.policyrex.flow

import co.paralleluniverse.fibers.Suspendable
import com.policyrex.contract.WalletsTransactionsContract
import com.policyrex.contract.WalletsTransactionsContract.Companion.TRANSACTION_CONTRACT_ID
import com.policyrex.state.WalletsTransactionsState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step


object WalletsTransactionsFlow {
    @InitiatingFlow
    @StartableByRPC
    class WalletsTransactionsInitiator(
            val policyREXNode: Party,
            val sender_id: String,
            val receive_id: String,
            val currency: String,
            val value: Int
    ) : FlowLogic<SignedTransaction>()
    {

        companion object {
            object GENERATING_TRANSACTION : Step("Generating transaction based on new IOU.")
            object VERIFYING_TRANSACTION : Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : Step("Signing transaction with our private key.")
            object GATHERING_SIGS : Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
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
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            val iouState = WalletsTransactionsState(serviceHub.myInfo.legalIdentities.first(), policyREXNode, sender_id, receive_id, currency, value)
            val txCommand = Command(WalletsTransactionsContract.Commands.Create(), iouState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(iouState, TRANSACTION_CONTRACT_ID)
                    .addCommand(txCommand)

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
            // Send the state to the counterparty, and receive it back with their signature.
            val otherPartyFlow = initiateFlow(policyREXNode)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow), GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    @InitiatedBy(WalletsTransactionsInitiator::class)
    class Acceptor(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an transaction." using (output is WalletsTransactionsState)
                    val iou = output as WalletsTransactionsState
                    "I won't accept with a value over 1." using (iou.value <= 1)
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}
