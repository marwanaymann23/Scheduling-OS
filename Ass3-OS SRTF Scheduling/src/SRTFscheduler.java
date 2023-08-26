
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;

class Process {
	int priority;
	int arrivalTime;
	int burstTime;
	int currentBurstTime;
	String name;
	int waitingTime;
	int turnAroundTime;

	Process(String name, int arrivalTime, int burstTime) {
		this.priority = burstTime;
		this.arrivalTime = arrivalTime;
		this.burstTime = burstTime;
		this.currentBurstTime = burstTime;
		this.name = name;
	}

	Process() {
	}
}

class Scheduler {
	int contextSwitching;
	ArrayList<Process> allProcesses = new ArrayList<Process>(); // All processes sorted according to arrival time
	ArrayList<Process> queue = new ArrayList<Process>(); // Processes sorted according to arrival time and priority
	ArrayList<Process> chart = new ArrayList<Process>(); // Gantt chart
	HashMap<Integer, String> gantt = new HashMap<Integer, String>(); // Full Sequence of processes
	
	// Class constructor initializes context switching time
	Scheduler(int contextSwitching) {
		this.contextSwitching = contextSwitching;
	}

	static class processComparator implements Comparator<Process> {
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

	// Adds a process
	void addProcess(Process process) {
		allProcesses.add(process);
	}

	// Checks for aging to handle starvation
	void checkAging(int timer) {
		for (Process process : queue) {
			if ((timer - process.arrivalTime) % 10 == 0 && process.priority > 0) {
				process.priority--;
			}
		}
	}

	void run() {
		// Sort all processes according to arrival time first
		Collections.sort(allProcesses, new Scheduler.processComparator());
		Process currentProcess = new Process();
		boolean switching = false;
		boolean getProcess = false;
		int switchTimer = 0;
		int remainingProcesses = allProcesses.size();
		int time = 0;

		while (remainingProcesses != 0) {
			checkAging(time);

			// If a process has arrived, place it in queue according to its priority
			for (Process process : allProcesses) {
				if (process.arrivalTime == time) {
					int i;
					for (i = queue.size() - 1; i >= 0 && process.priority < queue.get(i).priority; i--)
						;
					queue.add(i + 1, process);
				}
			}
			if (switching) {
				// switchTimer++;

				if (switchTimer >= contextSwitching) {
					switching = false;
					switchTimer = 0;
				} else {
					switchTimer++;
					time++;
					continue;
				}
			}
			// At first second take the process that arrived first
			if (time == 0) {
				currentProcess = queue.get(0);
			}
						
			// If current burst time is 0, the process has ended, do the required
			// calculations, add it to the chart,
			// remove it from queue, set getProcess boolean to true to take the upcoming
			// process from the queue
			if (currentProcess.currentBurstTime == 0) {
				currentProcess.turnAroundTime = (time) - currentProcess.arrivalTime;
				currentProcess.waitingTime = currentProcess.turnAroundTime - currentProcess.burstTime;

				if (queue.size() > 1) {
					switching = true;
				}
				chart.add(currentProcess);
				queue.remove(currentProcess);
				remainingProcesses--;
				getProcess = true;
			}
			// If getProcess is true pop a new process from the queue
			if(getProcess && queue.size() > 0) {
				currentProcess = queue.get(0);
				getProcess = false;
			} 
			else if (queue.size() > 0 && !queue.get(0).equals(currentProcess) && queue.get(0).currentBurstTime < currentProcess.currentBurstTime) {
				currentProcess = queue.get(0);
			}
			gantt.put(time, currentProcess.name);
			// Decrement burst time and priority			
			currentProcess.currentBurstTime--;
			currentProcess.priority--;
			time++;
		}
		print();
	}

	float averageWaitingTime() {
		float result = 0;
		for (Process process : allProcesses) {
			result += process.waitingTime;
		}
		return result / (float) allProcesses.size();
	}

	float averageTurnaroundTime() {
		float result = 0;
		for (Process process : allProcesses) {
			result += process.turnAroundTime;
		}
		return result / (float) allProcesses.size();
	}

	void print() {
		JFrame frame = new JFrame();
		frame.setTitle("Gantt Chart");
		
		// Process WT & TAT
		String[] columns = { "Process Name", "Turnaround Time", "Waiting Time" };
		String[][] processes = new String[chart.size() + 3][3];
		int i;
		for (i = 0; i < chart.size(); i++) {
			processes[i][0] = chart.get(i).name;
			processes[i][1] = Integer.toString(chart.get(i).turnAroundTime);
			processes[i][2] = Integer.toString(chart.get(i).waitingTime);
		}
		processes[i + 1][0] = "Avg turnaround time";
		processes[i + 1][1] = Float.toString(averageTurnaroundTime());
		processes[i + 2][0] = "Avg waiting time";
		processes[i + 2][1] = Float.toString(averageWaitingTime());
		JTable table = new JTable(processes, columns);
		table.setBounds(30, 40, 200, 300);
		JScrollPane sp = new JScrollPane(table);
		frame.setSize(900, 600);
		frame.getContentPane().add(sp, BorderLayout.SOUTH);
				
		// Full Sequence processes Array
		String[] ganttColumn = new String[gantt.size()];
		String[][] ganttRow = new String[1][ganttColumn.length];
		// loop to set the names of columns
		int counter = 0;
		ganttColumn[gantt.size() - 1] = "CT";
		for(int j = 0; j < gantt.size() - 1; j++) {
			// to get the name index by index
			for (Map.Entry<Integer, String> entry : gantt.entrySet()) {
	            if(counter == j) {
	            	ganttColumn[j] = entry.getValue();
	            }
	            counter++;
	        }
			counter = 0;		
		}
		for(int j = 0; j < gantt.size() - 1; j++) {
			// to get the name index by index
			for (Map.Entry<Integer, String> entry : gantt.entrySet()) {
	            if(counter == j) {
	            	ganttRow[0][j] = Integer.toString(entry.getKey());
	            }
	            if(j == gantt.size() - 2) {
	            	ganttRow[0][j + 1] = Integer.toString((entry.getKey()));
	            }
	            counter++;
	        }
			counter = 0;
		}
		
		JTable table2 = new JTable(ganttRow, ganttColumn);
		table2.setBounds(30, 40, 200, 300);
		JScrollPane sp2 = new JScrollPane(table2);
		frame.add(sp2, BorderLayout.CENTER);
		frame.validate();	
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
}
public class SRTFscheduler {

	public static void main(String[] args) {
		int numberOfProcesses;
		String processName;
		int arrivalTime;
		int burstTime;
		int contextSwitching;
		Scanner scanner = new Scanner(System.in);

		System.out.println("Enter context switching time.");
		contextSwitching = scanner.nextInt();
		scanner.nextLine();
		Scheduler scheduler = new Scheduler(contextSwitching);

		System.out.println("Enter number of processes.");
		numberOfProcesses = scanner.nextInt();
		scanner.nextLine();

		for (int i = 0; i < numberOfProcesses; i++) {
			System.out.println("Enter process name.");
			processName = scanner.next();
			scanner.nextLine();
			System.out.println("Enter process arrival time.");
			arrivalTime = scanner.nextInt();
			scanner.nextLine();
			System.out.println("Enter process burst time.");
			burstTime = scanner.nextInt();
			scanner.nextLine();

			Process newProcess = new Process(processName, arrivalTime, burstTime);
			scheduler.addProcess(newProcess);
		}
		scanner.close();

		scheduler.run();

	}

}
