import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.text.DecimalFormat;
import java.util.*;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class FS {
    private static final int NUM_VMS = 4;
    private static final int VM_PES = 1;
    private static final int CLOUDLET_LENGTH_CEILING = 1000;
    private static final int MIN_MIPS = 1000;
    private static final int MAX_MIPS = 2000;
    private static final int MIN_RAM = 2048;
    private static final int MAX_RAM = 4096;
    private static final int MIN_DISK = 1000000;
    private static final int MAX_DISK = 2000000;
    private static final int CLOUDLET_FILE_SIZE = 500;

    public static void main(String[] args) {
        Log.printLine("Starting FirstSenario...");

        try {
            int numBrokers = 1;
            boolean traceFlag = false;
            CloudSim.init(numBrokers, Calendar.getInstance(), traceFlag);

            List<Host> hostList = createHostList();
            createDatacenter(hostList);

            DatacenterBroker broker = createRoundRobinBroker();

            assert broker != null;
            List<Cloudlet> cloudletList = generateCloudlets(broker.getId());

            broker.submitCloudletList(cloudletList);

            List<Vm> vmList = createVms(broker.getId());
            broker.submitVmList(vmList);

            CloudSim.startSimulation();

            List<Cloudlet> completedCloudlets = broker.getCloudletReceivedList();

            printResult(completedCloudlets);

            CloudSim.stopSimulation();

            Log.printLine("finished!");
        } catch (Exception e) {
            Log.printLine("The simulation has been terminated due to an unexpected error");
            e.printStackTrace();
        }
    }

    private static DatacenterBroker createRoundRobinBroker() {
        try {
            return new DatacenterBroker("Broker") {
                int vmIndex = 0;

                @Override
                protected void submitCloudlets() {
                    int numVms = getVmsCreatedList().size();
                    int numCloudlets = getCloudletList().size();
                    int cloudletsSubmitted = 0;

                    while (cloudletsSubmitted < numCloudlets) {
                        Vm vm = getVmsCreatedList().get(vmIndex);
                        Cloudlet cloudlet = getCloudletList().get(cloudletsSubmitted);
                        cloudlet.setVmId(vm.getId());
//                        sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
                        double delay = cloudletsSubmitted * 0.1; // تاخیر بین هر ارسال بر حسب واحد زمان
                        send(getVmsToDatacentersMap().get(vm.getId()), delay, CloudSimTags.CLOUDLET_SUBMIT, cloudlet);

                        cloudletsSubmitted++;
                        vmIndex = (vmIndex + 1) % numVms;
                        Log.printLine("Submitted Cloudlet " + cloudlet.getCloudletId() + " to VM " + vm.getId());
                        long length=cloudlet.getCloudletLength(); // total length
                        long insize=cloudlet.getCloudletFileSize(); // input size
                        Long l1=length;
                        double tlength=l1.doubleValue();
                        Long l2=insize;
                        double input= l2.doubleValue();
                        double mip=vm.getMips();
                        int pcount=vm.getNumberOfPes();
                        long band=vm.getBw();
//                        Integer x=new Integer(pcount);
                        double pecount=pcount; //int to double conversion
                        Long b=band;
                        double bw=b.doubleValue(); // long to double conversion
//                        double exec=(tlength/mip*pecount)+(input/bw);
                        double exec=(tlength/mip);
                        Log.printLine("Estimate Time " + (exec) );
                    }
                }
            };
        } catch (Exception e) {
            Log.printLine("Error creating a broker.");
            return null;
        }
    }

    private static void createDatacenter(List<Host> hostList) {
        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.1;
        double costPerBw = 0.1;

        LinkedList<Storage> storageList = new LinkedList<>();

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        // create a Datacenter object
        try {
            new Datacenter("Datacenter", characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<Host> createHostList() {
        Random random = new Random();
        List<Host> hostList = new ArrayList<>(NUM_VMS);
        for (int i = 0; i < NUM_VMS; i++) {
            int mips = MIN_MIPS + random.nextInt(MAX_MIPS - MIN_MIPS + 1);
            int ram = MIN_RAM + random.nextInt(MAX_RAM - MIN_RAM + 1);
            long disk = MIN_DISK + random.nextInt(MAX_DISK - MIN_DISK + 1);

            List<Pe> peList = new ArrayList<>(VM_PES);
            for (int j = 0; j < VM_PES; j++) {
                peList.add(new Pe(j, new PeProvisionerSimple(mips)));
            }

            hostList.add(
                    new Host(
                            i,
                            new RamProvisionerSimple(ram),
                            new BwProvisionerSimple(10000),
                            disk,
                            peList,
                            new VmSchedulerTimeShared(peList)
                    )
            );
        }
        return hostList;
    }

    private static List<Vm> createVms(int brokerId) {
        Random random = new Random();
        List<Vm> vmList = new ArrayList<>(NUM_VMS);
        for (int i = 0; i < NUM_VMS; i++) {
            int mips = 256 + random.nextInt(MAX_MIPS - MIN_MIPS);
            int memory = 128 + random.nextInt(512);
            int disk = 256 + random.nextInt(1024);
            int size = 4000 + random.nextInt(7000);
            Vm vm = new Vm(
                    i,
                    brokerId,
                    mips,
                    VM_PES,
                    memory,
                    disk,
                    size,
                    "Xen",
                    new CloudletSchedulerSpaceShared()
            );
            vmList.add(vm);
        }
        return vmList;
    }

    private static List<Cloudlet> generateCloudlets(int brokerId) {
        Random random = new Random();
        List<Cloudlet> cloudletList = new ArrayList<>(24);
        for (int i = 0; i < 24; i++) {
            int cloudletLength = random.nextInt(CLOUDLET_LENGTH_CEILING) + 1;
            int cloudletFileSize = random.nextInt(CLOUDLET_FILE_SIZE) + 1;
            Cloudlet cloudlet = new Cloudlet(
                    i,
                    cloudletLength,
                    VM_PES,
                    cloudletFileSize,
                    cloudletFileSize,
                    new UtilizationModelFull(),
                    new UtilizationModelFull(),
                    new UtilizationModelFull()
            );
            cloudlet.setUserId(brokerId);
            cloudletList.add(cloudlet);
        }
        return cloudletList;
    }

    private static void printResult(List<Cloudlet> list) {
        List list1;
        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "Data center ID" + indent +
                "VM ID" + indent + "Time" + indent +
                "Start Time" + indent + "Finish Time" + indent + "STATUS");

        DecimalFormat dft = new DecimalFormat("###.##");
        Log.printLine(String.format("%-13s%-16s%-10s%-7s%-12s%-13s%-11s",
                "----------", "----------------  ", "-------", "------", "------------  ", "-------------  ", "---------"));


        Collections.sort(list, new CloudletSort());

        for (Cloudlet cloudlet : list) {
            Log.print(String.format("%-20d", cloudlet.getCloudletId()));
            Log.print(String.format("%-14d", cloudlet.getResourceId()));
            Log.print(String.format("%-8d", cloudlet.getVmId()));
            Log.print(String.format("%-11s", dft.format(cloudlet.getActualCPUTime())));
            Log.print(String.format("%-14s", dft.format(cloudlet.getExecStartTime())));
            Log.print(String.format("%-11s", dft.format(cloudlet.getFinishTime())));

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.printLine(String.format("%-11s", "SUCCESS"));
            } else {
                Log.printLine();
            }
        }
    }
}

