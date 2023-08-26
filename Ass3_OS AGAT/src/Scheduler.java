import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;

class Process {

	String name;
	int burstTime;
	int remainingBurstTime;
	int arrivalTime;
	int priority;
	int quantum;
	int AGATFactor;
	int iterations;
	int turnAroundTime;
	int waitingTime;
	ArrayList<Integer> quantumHistory = new ArrayList<Integer>();
	ArrayList<Integer> AGATHistory = new ArrayList<Integer>();

	public Process(String name, int burstTime, int arrivalTime, int priority, int quantum) {
		this.name = name;
		this.remainingBurstTime = this.burstTime = burstTime;
		this.arrivalTime = arrivalTime;
		this.priority = priority;
		this.quantum = quantum;
		this.quantumHistory.add(this.quantum);
		this.iterations = 0;
	}
}

class AGAT {

	static ArrayList<Process> processes = new ArrayList<Process>();
	static ArrayList<Process> chart = new ArrayList<Process>();
	// All except the dead
	static ArrayList<Process> futureExist = new ArrayList<Process>();
	static ArrayList<Process> readyQueue = new ArrayList<Process>();
	static ArrayList<String> excutionOrder = new ArrayList<String>();
	static ArrayList<Integer> excutionTimeOrder = new ArrayList<Integer>();

	// Process Active

	public static void addProcess(Process process) {
		processes.add(process);
		futureExist.add(process);
	}

	static int maxRemainingBurstTime;
	static float v1;
	static float v2;

	public AGAT() {
		maxRemainingBurstTime = 0;
	}

	// called at the first line of the running function and called only one time
	public static void calculationgV1() {
		int lastArrivalTime = 0;
		for (Process process : processes) {
			if (process.arrivalTime > lastArrivalTime) {
				lastArrivalTime = process.arrivalTime;
			}
		}

		if (lastArrivalTime > 10) {
			v1 = (float) (lastArrivalTime / 10.0);
		} else {
			v1 = 1;
		}
	}

	// to be called on every context switch
	public static void calculateV2Factor() {

		calculatingMaxRemainingTime();
		for (Process process : futureExist) {

			// Setting v2 values
			if (maxRemainingBurstTime > 10) {
				v2 = (float) (maxRemainingBurstTime / 10.0);
			} else {
				v2 = 1;
			}

			// Setting AGAT factor
			int term1 = 10 - process.priority;
			int term2 = (int) Math.ceil(process.arrivalTime / v1);
			int term3 = (int) Math.ceil(process.remainingBurstTime / v2);
			process.AGATFactor = term1 + term2 + term3;

			process.AGATHistory.add(process.AGATFactor);
		}
	}

	public static void calculatingMaxRemainingTime() {
		maxRemainingBurstTime = 0;
		for (Process process : processes) {
			if (process.remainingBurstTime > maxRemainingBurstTime) {
				maxRemainingBurstTime = process.remainingBurstTime;
			}
		}

	}

	public static Process minAGAT(Process activeProcess) {

		for (Process process : readyQueue) {
			if (process.AGATFactor < activeProcess.AGATFactor) {
				activeProcess = process;
			}
		}
		return activeProcess;

	}

	public static boolean preOrNotPre(Process process) {
		int fourtyPercent = (int) Math.round(((float) ((process.quantum * 40) / 100.0)));

		if (process.iterations >= fourtyPercent) {
			// pre
			return true;
		} else {
			// not pre
			return false;
		}
	}

	static class arrivingComparator implements Comparator<Process> {
		// If arrival times are equal sort according to priority
		// Else sort according to arrival time
		@Override
		public int compare(Process firstProcess, Process secondProcess) {
			if (firstProcess.arrivalTime == secondProcess.arrivalTime) {
				return (firstProcess.priority - secondProcess.priority);
			}
			return (firstProcess.arrivalTime - secondProcess.arrivalTime);
		}

	}

	static class factorComparator implements Comparator<Process> {
		// If arrival times are equal sort according to priority
		// Else sort according to arrival time
		@Override
		public int compare(Process firstProcess, Process secondProcess) {
			return (firstProcess.AGATFactor - secondProcess.AGATFactor);
		}

	}

	public static int totalBurstTime() {
		int sum = 0;
		for (Process process : processes) {
			sum += process.burstTime;
		}
		return sum;
	}

	static float averageWaitingTime() {
		float result = 0;
		for (Process process : processes) {
			result += process.waitingTime;
		}
		return result / (float) processes.size();
	}

	static float averageTurnaroundTime() {
		float result = 0;
		for (Process process : processes) {
			result += process.turnAroundTime;
		}
		return result / (float) processes.size();
	}

	public static void run() {
		calculationgV1();
		Collections.sort(processes, new AGAT.arrivingComparator());

		Process activeProcess = null;
		for (int time = 0; time < totalBurstTime(); time++) {

			// Checking for time arriving
			for (Process process : processes) {
				if (process.arrivalTime == time) {

					// Collections.sort(readyQueue, new AGAT.factorComparator());
					if (readyQueue.size() == 0) {
						readyQueue.add(process);
					} else {
						int i;
						for (i = 0; i < readyQueue.size(); i++) {
							if (process.AGATFactor < readyQueue.get(i).AGATFactor) {

								break;
							}
						}
						readyQueue.add(i, process);
					}
				}
			}

			if (readyQueue.size() == 0) {
				continue;
			}

			// Insertion and handling insertion conditions

			boolean newActiveInsertion = false;
			if (activeProcess == null) {// If its null // 1- at first iteration //2- when a process is done or quantum
										// is over
				activeProcess = readyQueue.get(0);
				newActiveInsertion = true;
				if (excutionOrder.size() == 0 || excutionOrder.get(excutionOrder.size() - 1) != activeProcess.name) {
					excutionOrder.add(activeProcess.name);
					excutionTimeOrder.add(time);
				}

			} else if (preOrNotPre(activeProcess)) {// pre-emtive case
				// if the new == the active
				if (minAGAT(activeProcess) != activeProcess) {// There is one has AGAT factor less than the active one
					// Removing the active process from the head of the queue and re-add it at the
					// end
					readyQueue.remove(activeProcess);
					readyQueue.add(activeProcess);

					// Increasing the removed quantum by the remaining quantum time by using
					// iteration and set iteration to zero
					if (futureExist.size() > 1) {
						activeProcess.quantum += (activeProcess.quantum - activeProcess.iterations);
						activeProcess.quantumHistory.add(activeProcess.quantum);
					}
					activeProcess.iterations = 0;

					activeProcess = minAGAT(activeProcess);

					if (excutionOrder.size() == 0
							|| excutionOrder.get(excutionOrder.size() - 1) != activeProcess.name) {
						excutionOrder.add(activeProcess.name);
						excutionTimeOrder.add(time);
					}

					newActiveInsertion = true;
				}
			}

			// calculating the new v2 and factor
			if (newActiveInsertion) {
				if (futureExist.size() > 1) {
					calculateV2Factor();
				}
				// Collections.sort(readyQueue, new AGAT.factorComparator());
			}

			// Making some activities on CPU

			// The end of the iteration

			activeProcess.iterations++;
			activeProcess.remainingBurstTime--;
			if (activeProcess.remainingBurstTime == 0) {// It is over

				activeProcess.turnAroundTime = (time + 1) - activeProcess.arrivalTime;
				activeProcess.waitingTime = activeProcess.turnAroundTime - activeProcess.burstTime;
				chart.add(activeProcess);
				readyQueue.remove(activeProcess);
				futureExist.remove((activeProcess));
				activeProcess = null;
			} else if (activeProcess.iterations == activeProcess.quantum) {// Quantum time is over

				readyQueue.remove(activeProcess);
				readyQueue.add(activeProcess);

				if (futureExist.size() > 1) {
					activeProcess.quantum += 2;
					activeProcess.quantumHistory.add(activeProcess.quantum);
				}
				activeProcess.iterations = 0;
				activeProcess = null;
			}
		}
		print();
	}

	static int MaxQuantumSize() {
		int max = 0;
		for (Process process : chart) {
			if (process.quantumHistory.size() > max)
				max = process.quantumHistory.size();
		}
		return max;
	}

	static int MaxAGATSize() {
		int max = 0;
		for (Process process : chart) {
			if (process.AGATHistory.size() > max)
				max = process.AGATHistory.size();
		}
		return max;
	}

	public static void print() {
		JFrame frame = new JFrame();
		JFrame frame2 = new JFrame();

		frame.setTitle("AGAT SCHEDULING");
		frame2.setTitle("AGAT SCHEDULING");

		// Gantt Chart Table
		String[] columns = { "Process Name", "Turnaround Time", "Waiting Time" };
		String[][] output = new String[chart.size() + 3][3];
		int i;
		for (i = 0; i < chart.size(); i++) {
			output[i][0] = chart.get(i).name;
			output[i][1] = Integer.toString(chart.get(i).turnAroundTime);
			output[i][2] = Integer.toString(chart.get(i).waitingTime);
		}
		output[i + 1][0] = "Avg turnaround time";
		output[i + 1][1] = Float.toString(averageTurnaroundTime());
		output[i + 2][0] = "Avg waiting time";
		output[i + 2][1] = Float.toString(averageWaitingTime());
		JTable table = new JTable(output, columns);
		table.setBounds(30, 40, 50, 80);
		JScrollPane sp = new JScrollPane(table);
		frame.setSize(900, 500);
		frame2.setSize(900, 500);
		frame.getContentPane().add(sp);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		// Quantum History Table
		String[] quantum = new String[MaxQuantumSize() + 1];
		String[][] quantumTable = new String[chart.size()][quantum.length + 1];
		// loop to set the name of each column in the table
		quantum[0] = "Process Name";
		for (int l = 1; l < quantum.length; l++) {
			quantum[l] = "Q" + l;
		}

		for (int j = 0; j < chart.size(); j++) {
			quantumTable[j][0] = chart.get(j).name + " ";
			for (int k = 1; k < chart.get(j).quantumHistory.size() + 1; k++) {
				quantumTable[j][k] = Integer.toString(chart.get(j).quantumHistory.get(k - 1));
			}
		}

		JTable table2 = new JTable(quantumTable, quantum);
		table2.setBounds(30, 40, 200, 300);
		JScrollPane sp2 = new JScrollPane(table2);
		frame2.add(sp2, BorderLayout.SOUTH);

		// AGAT History Table
		String[] agat = new String[MaxAGATSize() + 1];
		String[][] agatTable = new String[chart.size()][agat.length + 1];
		// loop to set the name of each column in the table
		agat[0] = "Process Name";
		for (int l = 1; l < agat.length; l++) {
			agat[l] = "A" + l;
		}

		for (int j = 0; j < chart.size(); j++) {
			agatTable[j][0] = chart.get(j).name + " ";
			for (int k = 1; k < chart.get(j).AGATHistory.size() + 1; k++) {
				agatTable[j][k] = Integer.toString(chart.get(j).AGATHistory.get(k - 1));
			}
		}
		JTable table3 = new JTable(agatTable, agat);
		table3.setBounds(30, 40, 200, 300);
		JScrollPane sp3 = new JScrollPane(table3);
		frame2.add(sp3, BorderLayout.CENTER);
		frame2.setVisible(true);
		frame2.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame2.validate();

		for (int q = 0; q < excutionOrder.size(); q++) {
			System.out.print(excutionOrder.get(q) + " ");
		}

		System.out.println();

		for (int q = 0; q < excutionTimeOrder.size(); q++) {
			System.out.print(excutionTimeOrder.get(q) + " ");
		}
		System.out.print(totalBurstTime());
	}

}

public class Scheduler {

	static public void main(String[] args) {
		int burstTime;
		int arrivalTime;
		int priority;
		int quantum;
		String color;
		Scanner scanner = new Scanner(System.in);

		AGAT agat = new AGAT();

		System.out.println("Enter number of processes.");
		int numberOfProcesses = scanner.nextInt();
		scanner.nextLine();

		for (int i = 0; i < numberOfProcesses; i++) {
			System.out.println("Enter process name.");
			String name = scanner.next();
			scanner.nextLine();
			System.out.println("Enter process arrival time.");
			arrivalTime = scanner.nextInt();
			scanner.nextLine();
			System.out.println("Enter process burst time.");
			burstTime = scanner.nextInt();
			scanner.nextLine();

			System.out.println("Enter process priority.");
			priority = scanner.nextInt();
			scanner.nextLine();

			System.out.println("Enter process quantum.");
			quantum = scanner.nextInt();
			scanner.nextLine();

			Process newProcess = new Process(name, burstTime, arrivalTime, priority, quantum);
			AGAT.addProcess(newProcess);
		}
		scanner.close();

		AGAT.run();

	}

}
