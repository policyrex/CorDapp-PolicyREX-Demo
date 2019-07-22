package com.policyrex.contract

import com.policyrex.state.UserState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class UserContract : Contract {
    companion object {
        @JvmStatic
        val USER_CONTRACT_ID = "com.policyrex.contract.UserContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<UserContract.Commands>()
        when (command.value) {
            is UserContract.Commands.UserApplication -> {
                requireThat {
                    // Generic constraints around the Claim transaction.
                    "No inputs should be consumed when creating Claim Application." using (tx.inputs.isEmpty())
                    "Only one output state should be created." using (tx.outputs.size == 1)
                    val out = tx.outputsOfType<UserState>().single()
                    "The Applicant and the Insurance Company cannot be the same entity." using (out.user_node != out.policyrex_node)
                    "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

                    "Username must be not null" using (out.user_name != null)
                }
            }

            is UserContract.Commands.UserTest -> {
                requireThat {
                    // Generic constraints around the Claim transaction.
                    "One input should be consumed" using (tx.inputs.size==1)
                }
            }

            is UserContract.Commands.UserResponse -> {
                requireThat {
                    // Generic constraints around the Claim transaction.
                    "Two output states should be created." using (tx.outputs.size == 2)
                    val input = tx.inputsOfType<UserState>().single()
                    val out = tx.outputsOfType<UserState>().single()
                    "The Applicant and the Insurance Company cannot be the same entity." using (out.policyrex_node != out.user_node)
                    "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

                }
            }
        }
    }

    /**
     * This contract implements two commands.
     */
    interface Commands : CommandData {
        class UserApplication : Commands
        class UserResponse : Commands
        class UserTest : Commands
    }
}
