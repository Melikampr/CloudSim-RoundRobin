import org.cloudbus.cloudsim.Cloudlet;

public class CloudletSort implements java.util.Comparator<Cloudlet> {
    @Override
    public int compare(Cloudlet a, Cloudlet b) {
        return a.getCloudletId() - b.getCloudletId();
    }
}
