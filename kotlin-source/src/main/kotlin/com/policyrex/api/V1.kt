package com.policyrex.api

import com.policyrex.flow.CreateClaimFlow
import com.policyrex.flow.UserCreateNewFlow
import com.policyrex.flow.WalletsTransactionsFlow
import com.policyrex.state.UserState
import com.policyrex.state.WalletsTransactionsState
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.IdentityService
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.*
val SERVICE_NAMES = listOf("Notary", "Network Map Service")

// This API is accessible from /api/v1. All paths specified below are relative to it.
@Path("v1")
class V1(private val rpcOps: CordaRPCOps) {

    private val myLegalName: CordaX500Name = rpcOps.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<V1>()
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */

    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = rpcOps.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    /**
     * Add New User
     */
    @PUT
    @Path("user")
    fun createUser(
            @QueryParam("first_name") first_name: String,
            @QueryParam("last_name") last_name: String,
            @QueryParam("user_name") user_name: String,
            @QueryParam("id_card") id_card: String,
            @QueryParam("email") email: String,
            @QueryParam("phone") phone: String,
            @QueryParam("address") address: String,
            @QueryParam("status") status: Int
    ): Response
    {
        val status: Int = 0
        val companyName=CordaX500Name.parse("O=PolicyREX, L=Toronto, C=CA")
        val policyREXNode = rpcOps.wellKnownPartyFromX500Name(companyName) ?:
        return Response.status(BAD_REQUEST).entity("Company named $companyName cannot be found.\n").build()

        if (first_name == null) {
            return Response.status(BAD_REQUEST).entity("First name is missing. \n").build()
        }
        if (last_name == null) {
            return Response.status(BAD_REQUEST).entity("Last name is missing. \n").build()
        }

        return try {
            val signedTx = rpcOps
                    .startFlowDynamic(UserCreateNewFlow.UserInitiator::class.java,policyREXNode, first_name, last_name, user_name, id_card, email, phone, address, status).returnValue.getOrThrow()
            Response.status(CREATED).entity("${signedTx.id}\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

    /**
     * Get users list
     *
     */
    @GET
    @Path("users")
    @Produces(MediaType.APPLICATION_JSON)
    fun getUsers() = rpcOps.vaultQueryBy<UserState>().states


    @PUT
    @Path("wallet_sent")
    fun sent(
            @QueryParam("sender_id") sender_id: String,
            @QueryParam("receive_id") receive_id: String,
            @QueryParam("currency") currency: String,
            @QueryParam("value") value: Int
    ): Response
    {
        val status: Int = 0
        val companyName=CordaX500Name.parse("O=PolicyREX, L=Toronto, C=CA")
        val policyREXNode = rpcOps.wellKnownPartyFromX500Name(companyName) ?:
        return Response.status(BAD_REQUEST).entity("Company named $companyName cannot be found.\n").build()

        if (value <= 0 ) {
            return Response.status(BAD_REQUEST).entity("Query parameter value must be non-negative.\n").build()
        }
        if (receive_id == null) {
            return Response.status(BAD_REQUEST).entity("Please enter Receive info.\n").build()
        }

        return try {
            val signedTx = rpcOps
                    .startFlowDynamic(WalletsTransactionsFlow.WalletsTransactionsInitiator::class.java,policyREXNode, sender_id, receive_id, currency, value).returnValue.getOrThrow()
            Response.status(CREATED).entity("${signedTx.id}\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

    @GET
    @Path("wallet_transactions")
    @Produces(MediaType.APPLICATION_JSON)
    fun getWalletTransactions() = rpcOps.vaultQueryBy<WalletsTransactionsState>().states

    @PUT
    @Path("create-claim")
    fun createClaim(@QueryParam("value") value: Int,
                        @QueryParam("userID") userID: String,
                        @QueryParam("userName") userName: String,
                        @QueryParam ("address") address: String): Response
    {
        val insuranceStatus: String = "RECEIVED"
        val companyName=CordaX500Name.parse("O=Insurer, L=New York, C=US")
        val insurerNode = rpcOps.wellKnownPartyFromX500Name(companyName) ?:
        return Response.status(BAD_REQUEST).entity("Company named $companyName cannot be found.\n").build()

        if (value <= 0) {
            return Response.status(BAD_REQUEST).entity(" parameter 'value' must be non-negative.\n").build()
        }

        if (userName == null) {
            return Response.status(BAD_REQUEST).entity("Full Name is missing. \n").build()
        }
        if (address == null) {
            return Response.status(BAD_REQUEST).entity("Address is missing. \n").build()
        }

        return try {
            val signedTx = rpcOps
                    .startFlowDynamic(CreateClaimFlow.ClaimInitiator::class.java,insurerNode,value,userID,userName,address,insuranceStatus).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

}
