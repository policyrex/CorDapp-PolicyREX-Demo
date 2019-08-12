# The PolicyREX CorDapp - This is training version

#### Interacting with the webserver

The static webpage is served on:

    http://localhost:10009/api/v1

New User:

    http://localhost:10009/api/v1/user?first_name=Jay&last_name=Nguyen&user_name=jay&id_card=67887688&email=demo@yahoo.com&phone=9823949824&address=aldjflkasjdfkljasf&status=0

User List:

    http://localhost:10009/api/v1/users

New Transaction

    http://localhost:10009/api/v1/wallet_sent?sender_id=1&receive_id=2&currency=USD&value=1

Transactions List

    http://localhost:10015/api/v1/wallet_transactions