package com.policyrex.contract

import com.policyrex.state.PolicyREXUserState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction


class PolicyREXUserContract : Contract {
    companion object {
        @JvmStatic
            val POLICYREXUSER_CONTRACT_ID = "com.policyrex.contract.PolicyREXUserContract"
    }

        /**
         * The verify() function of all the states' contracts must not throw an exception for a transaction to be
         * considered valid.
         */

        override fun verify(tx: LedgerTransaction) {
            val command = tx.commands.requireSingleCommand<PolicyREXUserContract.Commands>()
            when (command.value) {
                is PolicyREXUserContract.Commands.PolicyREXUser -> {
                    requireThat {
                        "Underwriting Transaction should have one input." using (tx.inputs.size==1)
                        "Two output states should be created." using (tx.outputs.size == 2)
                        val out = tx.outputsOfType<PolicyREXUserState>().single()
                        "Party cannot be same entity." using (out.user_node != out.policyrex_node)
                        "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

                    }
                }

                is PolicyREXUserContract.Commands.PolicyREXUserEvaluation -> {
                    requireThat {
                        "Only one output state should be created." using (tx.outputs.size == 1)
                        val input = tx.inputsOfType<PolicyREXUserState>().single()
                        val out = tx.outputsOfType<PolicyREXUserState>().single()
                        "The Insurance Company and the Underwriter Party cannot be same entity." using (out.user_node != out.policyrex_node)
                        "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

                    }
                }
                is PolicyREXUserContract.Commands.PolicyREXUsertatus -> {
                    requireThat {
                        "Status Transaction should have Two input." using (tx.inputs.size==2)
                    }
                }
            }
        }

        /**
         * This contract implements two commands.
         */
        interface Commands : CommandData {
            class PolicyREXUser : Commands
            class PolicyREXUserEvaluation : Commands
            class PolicyREXUsertatus : Commands
        }
    }
