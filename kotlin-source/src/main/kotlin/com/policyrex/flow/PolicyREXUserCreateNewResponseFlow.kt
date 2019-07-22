package com.policyrex.flow

import co.paralleluniverse.fibers.Suspendable
import com.policyrex.contract.PolicyREXUserContract
import com.policyrex.contract.UserContract
import com.policyrex.state.UserState
import com.policyrex.state.PolicyREXUserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

object PolicyREXUserCreateNewResponseFlow {
    @InitiatingFlow
    @StartableByRPC
    class PolicyREXUserCreateNewResponseFlowInitiator(
            val userNode: Party,
            val firt_name: String,
            val last_name: String,
            val user_name: String,
            val id_card: String,
            val email: String,
            val phone: String,
            val address: String,
            val value: Int
    ) : FlowLogic<SignedTransaction>() {

        companion object {
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying User")
            object COMPANY_RESPONSE : ProgressTracker.Step("Insurance Company responds to Applicant")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
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
            progressTracker.currentStep = COMPANY_RESPONSE
            // Generate an unsigned transaction.
            val inputUserResponseState = serviceHub.vaultService.queryBy<UserState>().states.singleOrNull{ it.state.data.user_name == user_name } ?: throw FlowException("No state found in the vault")
            val email= inputUserResponseState.state.data.email
            val address = inputUserResponseState.state.data.address
            val user_id =inputUserResponseState.state.data.linearId

            val inputPolicyREXUserState = serviceHub.vaultService.queryBy<PolicyREXUserState>().states.singleOrNull{ it.state.data.user_name == user_name } ?: throw FlowException("No state found in the vault")
            //val referenceID=inputUnderwritingState.state.data.linearId.id.toString()
            val policyREXNode= inputPolicyREXUserState.state.data.policyrex_node
            val id= inputPolicyREXUserState.state.data.linearId
            //val status= "$insuranceStatus ,Sent"
            val status= 1

            val outputPolicyREXUserStateRef = PolicyREXUserState(serviceHub.myInfo.legalIdentities.first(), policyREXNode, firt_name, last_name, user_name, id_card, email, phone, address, status, id)
            val claimState = UserState(userNode, serviceHub.myInfo.legalIdentities.first(), firt_name, last_name, user_name, id_card, email, phone, address, status, user_id)
            val txCommand = Command(UserContract.Commands.UserResponse(), claimState.participants.map { it.owningKey })
            val statusCommand=Command(PolicyREXUserContract.Commands.PolicyREXUsertatus(),outputPolicyREXUserStateRef.participants.map { it.owningKey })

            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(claimState, UserContract.USER_CONTRACT_ID)
                    .addOutputState(outputPolicyREXUserStateRef, PolicyREXUserContract.POLICYREXUSER_CONTRACT_ID)
                    .addInputState(inputUserResponseState)
                    .addInputState(inputPolicyREXUserState)
                    .addCommand(txCommand)
                    .addCommand(statusCommand)

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
            val underwriterPartyFlow=initiateFlow(policyREXNode)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow,underwriterPartyFlow), GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))

        }
    }

    @InitiatedBy(PolicyREXUserCreateNewResponseFlowInitiator::class)
    class PolicyREXUserCreateNewResponse(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                }
            }
            return subFlow(signTransactionFlow)
        }
    }
}