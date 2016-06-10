# Bitcoin Transaction Fee Booster

This program creates the transactions necessary for users to ensure that their transactions are confirmed quickly. 

## The Fee Boosting Methods

### Replace-By-Fee (RBF)

RBF is a method of increasing the transaction fee by having the sender resend the transaction with a higher fee. The Fee Booster will take the transaction in question and produce another transaction which has a higher fee. It deducts the fee from an output that the user selected.

### Child-Pays-For-Parent (CPFP)

CPFP is a method of increasing the transaction fee by having the receiver create another transaction whose fee covers that of the parent's. The Fee Booster will produce a transaction spending from a user selected output and send to a user specified address. 

## Security

The Fee Booster does not handle private keys (yet). It simply produces the unsigned transaction and asks the user to sign the transaction. Then that transaction can be broadcast through the Fee Booster.

## License

This Project is under the GNU General Public License version 3.
