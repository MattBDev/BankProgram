import java.net.*;
import java.io.*;

public class BankAccess implements Runnable {
	
	boolean dir;
	
	BufferedReader br;
	BufferedWriter bw;
	
	

	//Account accnt;
	//Make all pin checking done by account from now on
	private class Account {
		
		private int bal;
		private int pin;
		private String name;
		
		public Account(int bal, int pin, String name) {
			this.bal = bal;
			this.pin = pin;
			this.name = name;
		}
		
		public void deposit(int amt) throws BankException {
			if (amt >= 0 && (amt + bal) > bal) {
				bal += amt;
			} else {
				throw new BankException("Invalid amount; maximum size exceeded or negative number");
			}
		}
		
		//throw withdraw error: wrong pin, or invalid amount
		public void withdraw(int amt) throws BankException {
			
			//TODO: distinguish between and amt errors
			if (amt <= bal) {
				bal -= amt;
			} else {
				throw new BankException("Insufficient funds.");
			}
		}
		
		public String getName() {
			return name;
		}
		
		//TODO: probably remove this method
		public int getPin() {
			return pin;
		}
		
		public int getBal() {
			return bal;
		}
		
	}
	
	//call getMessage() to get details;
	private class BankException extends Exception {
		public BankException(String msg) {
			super(msg);
		}
	}
	

	public BankAccess(Socket s, boolean direct) throws IOException {
		dir = direct;
		
		br = new BufferedReader(new InputStreamReader(s.getInputStream()));
		bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
	}
	
	@Override
	public void run() {
				
		while (true) {
		
			write("hello");
			
			String in = read();
			
			String cmd[] = in.split("\\s+");
			Account acct = null;
			
			if (cmd.length > 1) {
				try {
					acct = buildAccount(cmd[1]);
				} catch (BankException e) {
					write(e.getMessage());
				}
			} else {
				write("Input command is not in a recognized form. Commands require at least two arguments");
			}
			
			
			if (acct != null) {
				try {
					parseCommand(cmd, acct);
				} catch (BankException e) {
					write(e.getMessage());
				}
			} else {
				write("Account not found");
			}
		
		}
				
	}
	
	private int parseLine(String[] in, String match) throws BankException {
		String error = "Error: Account file formatted incorrectly";
		if (in.length == 2 && in[0].equals(match + ":")) {
			try {
				return Integer.parseInt(in[1]);
			} catch (NumberFormatException e) {
				//technically superfluous, as it would just hit the next throw
				// anyways
				throw new BankException(error);
			}
		}
		throw new BankException(error);
	}
	
	//TODO: clean this up dramatically
	//Do error handling/throwing
	private Account buildAccount(String name) throws BankException {
		
		BufferedReader in = null;
		String s_bal[];
		String s_pin[];
		
		try {
			//prevent them from opening any other potentially malicious files
			if (!(name.equals("Jason") || name.equals("Matthew"))) {
				throw new BankException("Account name not recognized");
			}
			
			in = new BufferedReader(new FileReader((name + ".acct")));

			s_pin = in.readLine().split("\\s+");
			s_bal = in.readLine().split("\\s+");
			
			in.close();
			
			if (s_bal != null && s_pin != null) {
				
				int bal = parseLine(s_bal, "bal");
				int pin = parseLine(s_pin, "pin");
				
				return new Account(bal, pin, name);
				
			} else {
				throw new BankException("Error: Account file formatted incorrectly");
			}
						
		} catch (IOException e) {
			throw new BankException("Error: Account file not found");
		}
	}
	
	private void parseCommand(String[] cmd, Account acct) throws BankException {
		switch (cmd[0]) {
			case "getBalance":
				getBal(cmd, acct);
				break;
			case "deposit":
				deposit(cmd, acct);
				break;
			case "withdraw":
				withdraw(cmd, acct);
				break;
			default:
				throw new BankException("Command not recognized. Recognized commands are \"getBalance\", \"deposit\", and \"withdraw\"");
		}
		updateAccount(acct);
	}
	
	private void updateAccount(Account acct) throws BankException {
		
		BufferedWriter out = null;
		String s_bal[];
		String s_pin[];
		
		try {
			//prevent them from opening any other potentially malicious files
			if (!(acct.getName().equals("Jason") || acct.getName().equals("Matthew"))) {
				throw new BankException("Account name not recognized");
			}
			
			out = new BufferedWriter(new FileWriter((acct.getName() + ".acct")));
			
			out.write("pin: " + String.valueOf(acct.getPin()));
			out.newLine();
			out.flush();
			out.write("bal: " + String.valueOf(acct.getBal()));
			out.flush();
			
			out.close();
			
		} catch (IOException e) {
			throw new BankException("Error: Account file not found");
		}
	}
	
	private void getBal(String cmd[], Account acct) throws BankException {
		checkPin(acct);
		write(String.valueOf(acct.getBal()));
	}
	
	private void deposit(String cmd[], Account acct) throws BankException {
		if (dir) {
			if (cmd.length == 3) {
				int amt = parseAmt(cmd[2]);
				acct.deposit(amt);
				write(String.valueOf(acct.getBal()));
			} else {
				throw new BankException("Invalid number of arguments. Two arguments (account, amount) are used by this command.");
			}
		} else {
			throw new BankException("Deposits can only be made via a direct connection.");
		}
	}
	
	private void withdraw(String cmd[], Account acct) throws BankException {
		if (cmd.length == 3) {
			int amt = parseAmt(cmd[2]);
			checkPin(acct);
			acct.withdraw(amt);
			write(String.valueOf(acct.getBal()));
			
		} else {
			throw new BankException("Invalid number of arguments. Two arguments (account, amount) are used by this command.");
		}
	}
	
	//TODO: change this to double
	private int parseAmt(String amt) throws BankException {
		try {
			return Integer.parseInt(amt);
		} catch (NumberFormatException e){
			throw new BankException("Invalid amount; enter only integers");
		}
	}
	
	public boolean checkPin(Account acct) throws BankException {
		String in;
		int pin_in;
		//TODO: add limit on PIN guesses. When reached, return false; failed PIN
		while (true) {
			write("pin?");
			in = read();
			if (in.length() != 4) {
				throw new BankException("Invalid Format: Enter a four digit (0-9) integer");
			}
			try {
				pin_in = Integer.parseInt(in);
				if (pin_in == acct.getPin()) {
					//write(acct.getPin());
					return true;
				} else {
					write("Invalid PIN");
				}
			} catch (NumberFormatException e) {
				throw new BankException("Invalid Format: Enter a four digit (0-9) integer");
			}
		}
	}
	
	//TODO: handle IO exceptions
	private void write(String msg) {
				
		try {
			bw.write(msg);
			bw.newLine();
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//Do much better handling here
	private String read() {
		try {
			String in = br.readLine();			
			return in;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}