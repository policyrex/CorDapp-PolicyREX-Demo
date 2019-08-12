package com.policyrex.contract

import com.policyrex.state.ClaimState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class ClaimContract : Contract {
    companion object {
        @JvmStatic
        val CLAIM_CONTRACT_ID = "com.policyrex.contract.ClaimContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<ClaimContract.Commands>()
        when (command.value) {
            is ClaimContract.Commands.ClaimApplication -> {
                requireThat {
                    "No inputs should be consumed when creating Claim Application." using (tx.inputs.isEmpty())
                    "Only one output state should be created." using (tx.outputs.size == 1)
                    val out = tx.outputsOfType<ClaimState>().single()
                    "The Applicant and the Insurance Company cannot be the same entity." using (out.applicantNode != out.insurerNode)
                    "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))
                    "Valuation of property > 0." using (out.value > 0)
                }
            }

            is ClaimContract.Commands.ClaimTest -> {
                requireThat {
                    // Generic constraints around the Claim transaction.
                    "One input should be consumed" using (tx.inputs.size==1)
                }
            }

            is ClaimContract.Commands.ClaimResponse -> {
                requireThat {
                    "Two output states should be created." using (tx.outputs.size == 2)
                    val input = tx.inputsOfType<ClaimState>().single()
                    val out = tx.outputsOfType<ClaimState>().single()
                    "The Applicant and the Insurance Company cannot be the same entity." using (out.insurerNode != out.applicantNode)
                    "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

                }
            }
        }
    }

    /**
     * This contract implements two commands.
     */
    interface Commands : CommandData {
        class ClaimApplication : Commands
        class ClaimResponse : Commands
        class ClaimTest : Commands
    }
}
