package org.fdroid.fdroid.data;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.text.TextUtils;
import android.util.Log;
import org.apache.commons.io.IOUtils;
import org.fdroid.fdroid.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
public class DBHelperTest {
    static final String TAG = "DBHelperTest";

    private List<String> getReposFromXml(String xml) throws IOException, XmlPullParserException {
        File additionalReposXml = File.createTempFile("." + context.getPackageName() + "-DBHelperTest_",
                "_additional_repos.xml");
        Log.i(TAG, "additionalReposXml: " + additionalReposXml);

        FileOutputStream outputStream = new FileOutputStream(additionalReposXml);
        outputStream.write(xml.getBytes());
        outputStream.close();

        // Now parse that xml file
        return DBHelper.parseAdditionalReposXml(additionalReposXml);
    }

    protected Context context;

    @Before
    public final void setupBase() {
        context = InstrumentationRegistry.getContext();
    }

    @Test
    public void parseAdditionalReposXmlAllOneLineTest() throws IOException, XmlPullParserException {
        String oneRepoXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>" +
                "<string-array name=\"default_repos\">\n" +
                "<!-- name -->" +
                "<item>F-Droid</item>" +
                "<!-- address -->" +
                "<item>https://f-droid.org/repo</item>" +
                "<!-- description -->" +
                "<item>The official F-Droid repository. Applications in this repository are mostly built" +
                "directory from the source code. Some are official binaries built by the original" +
                "application developers - these will be replaced by source-built versions over time." +
                "</item>" +
                "<!-- version -->" +
                "<item>13</item>" +
                "<!-- enabled -->" +
                "<item>1</item>" +
                "<!-- push requests -->" +
                "<item>ignore</item>" +
                "<!-- pubkey -->" +
                "<item>" +
                "3082035e30820246a00302010202044c49cd00300d06092a864886f70d01010505003071310b30090603550406130255" +
                "</item>" +
                "</string-array>" +
                "</resources>";
        List<String> repos = getReposFromXml(oneRepoXml);
        assertEquals("Should contain one repo's worth of items", DBHelper.REPO_XML_ITEM_COUNT, repos.size());
    }

    @Test
    public void parseAdditionalReposXmlIncludedPriorityTest() throws IOException, XmlPullParserException {
        String wrongXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<resources>" +
                "<string-array name=\"default_repos\">\n" +
                "<!-- name -->" +
                "<item>F-Droid</item>" +
                "<!-- address -->" +
                "<item>https://f-droid.org/repo</item>" +
                "<!-- description -->" +
                "<item>The official F-Droid repository. Applications in this repository are mostly built" +
                "directory from the source code. Some are official binaries built by the original" +
                "application developers - these will be replaced by source-built versions over time." +
                "</item>" +
                "<!-- version -->" +
                "<item>13</item>" +
                "<!-- enabled -->" +
                "<item>1</item>" +
                "<!-- priority -->" +
                "<item>1</item>" +
                "<!-- push requests -->" +
                "<item>ignore</item>" +
                "<!-- pubkey -->" +
                "<item>" +
                "3082035e30820246a00302010202044c49cd00300d06092a864886f70d01010505003071310b3009060355040613025" +
                "</item>" +
                "</string-array>" +
                "</resources>";
        getReposFromXml(wrongXml);
        List<String> repos = getReposFromXml(wrongXml);
        assertEquals("Should be empty", 0, repos.size());
    }

    @Test(expected = XmlPullParserException.class)
    public void parseAdditionalReposXmlDoubleTagTest() throws IOException, XmlPullParserException {
        String wrongXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<resources>"
                + "<string-array name=\"additional_repos\">"
                + "<!-- address -->"
                + "<item><item>https://www.oem0.com/yeah/repo</item>"
                + "<!-- description -->"
                + "<item>I'm the first oem repo.</item>"
                + "<!-- version -->"
                + "<item>22</item>"
                + "<!-- enabled -->"
                + "<item>1</item>"
                + "<!-- push requests -->"
                + "<item>ignore</item>"
                + "<!-- pubkey -->"
                + "<item>fffff2313aaaaabcccc111</item>"
                + "</string-array>"
                + "</resources>";
        getReposFromXml(wrongXml);
        fail("Invalid xml read successfully --> Wrong");
    }

    @Test(expected = XmlPullParserException.class)
    public void parseAdditionalReposXmlMissingStartTagTest() throws IOException, XmlPullParserException {
        String wrongXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<item>https://www.oem0.com/yeah/repo</item>"
                + "<!-- description -->"
                + "<item>I'm the first oem repo.</item>"
                + "<!-- version -->"
                + "<item>22</item>"
                + "<!-- enabled -->"
                + "<item>1</item>"
                + "<!-- push requests -->"
                + "<item>ignore</item>"
                + "<!-- pubkey -->"
                + "<item>fffff2313aaaaabcccc111</item>"
                + "</string-array>"
                + "</resources>";
        getReposFromXml(wrongXml);
        fail("Invalid xml read successfully --> Wrong");
    }

    @Test
    public void parseAdditionalReposXmlWrongCountTest() throws IOException, XmlPullParserException {
        String wrongXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<resources>"
                + "<string-array name=\"default_repos\"><item>foo</item></string-array>"
                + "</resources>";
        List<String> repos = getReposFromXml(wrongXml);
        assertEquals("Should be empty", 0, repos.size());
    }

    /**
     * Parse valid xml but make sure that only <item> tags are parsed
     */
    @Test
    public void parseAdditionalReposXmlSloppyTest() throws IOException, XmlPullParserException {
        InputStream input = TestUtils.class.getClassLoader().getResourceAsStream("ugly_additional_repos.xml");
        String validXml = IOUtils.toString(input, "UTF-8");

        List<String> repos = getReposFromXml(validXml);
        assertEquals(2 * DBHelper.REPO_XML_ITEM_COUNT, repos.size());
        assertEquals("Repo Name", repos.get(8));
        assertEquals("https://www.oem0.com/yeah/repo", repos.get(9));
    }

    @Test
    public void parseAdditionalReposXmlPositiveTest() throws IOException {
        InputStream input = TestUtils.class.getClassLoader().getResourceAsStream("additional_repos.xml");
        String reposXmlContent = IOUtils.toString(input, "UTF-8");

        List<String> additionalRepos;
        try {
            additionalRepos = getReposFromXml(reposXmlContent);
        } catch (IOException io) {
            fail("IOException. Failed parsing xml string into repos.");
            return;
        } catch (XmlPullParserException xppe) {
            fail("XmlPullParserException. Failed parsing xml string into repos.");
            return;
        }

        // We should have loaded these repos
        List<String> oem0 = Arrays.asList(
                "oem0Name",
                "https://www.oem0.com/yeah/repo",
                "I'm the first oem repo.",
                "22",
                "1",
                "0",  // priority is inserted by DBHelper.parseAdditionalReposXml()
                "ignore",
                "fffff2313aaaaabcccc111");
        List<String> oem1 = Arrays.asList(
                "oem1MyNameIs",
                "https://www.mynameis.com/rapper/repo",
                "Who is the first repo?",
                "22",
                "0",
                "0",  // priority is inserted by DBHelper.parseAdditionalReposXml()
                "ignore",
                "ddddddd2313aaaaabcccc111");
        List<String> shouldBeRepos = new LinkedList<>();
        shouldBeRepos.addAll(oem0);
        shouldBeRepos.addAll(oem1);

        assertEquals(additionalRepos.size(), shouldBeRepos.size());
        for (int i = 0; i < additionalRepos.size(); i++) {
            assertEquals(shouldBeRepos.get(i), additionalRepos.get(i));
        }
    }

    @SuppressWarnings("LineLength")
    @Test
    public void canAddAdditionalRepos() throws IOException {
        File oemEtcDir = new File("/oem/etc");
        File oemEtcPackageDir = new File(oemEtcDir, context.getPackageName());
        if (!oemEtcPackageDir.canWrite() || !oemEtcDir.canWrite()) {
            if (oemEtcDir.canWrite() || new File("/").canWrite()) {
                oemEtcPackageDir.mkdirs();
            }
            if (TextUtils.isEmpty(System.getenv("CI")) && !oemEtcPackageDir.isDirectory()) {
                Log.e(TAG, "Cannot create " + oemEtcDir + ", skipping test!");
                return;
            }
        }

        File additionalReposXmlFile = new File(oemEtcPackageDir, "additional_repos.xml");
        FileOutputStream outputStream = new FileOutputStream(additionalReposXmlFile);
        outputStream.write(("<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<resources>"
                + "<string-array name=\"default_repos\">"
                + "<!-- name -->"
                + "<item>oem0Name</item>"
                + "<!-- address -->"
                + "<item>https://www.oem0.com/yeah/repo</item>"
                + "<!-- description -->"
                + "<item>I'm the first oem repo.</item>"
                + "<!-- version -->"
                + "<item>22</item>"
                + "<!-- enabled -->"
                + "<item>1</item>"
                + "<!-- push requests -->"
                + "<item>ignore</item>"
                + "<!-- pubkey -->"
                + "<item>fffff2313aaaaabcccc111</item>"

                + "<!-- name -->"
                + "<item>oem1MyNameIs</item>"
                + "<!-- address -->"
                + "<item>https://www.mynameis.com/rapper/repo</item>"
                + "<!-- description -->"
                + "<item>Who is the first repo?</item>"
                + "<!-- version -->"
                + "<item>22</item>"
                + "<!-- enabled -->"
                + "<item>0</item>"
                + "<!-- push requests -->"
                + "<item>ignore</item>"
                + "<!-- pubkey -->"
                + "<item>ddddddd2313aaaaabcccc111</item>"
                + "</string-array>"
                + "</resources>").getBytes());
        outputStream.close();

        try {
            List<String> initialRepos = DBHelper.loadInitialRepos(context);

            // Construct the repos that we should have loaded
            List<String> oem0 = Arrays.asList("oem0Name", "https://www.oem0.com/yeah/repo", "I'm the first oem repo.",
                    "22", "1", "0", "ignore", "fffff2313aaaaabcccc111");
            List<String> oem1 = Arrays.asList("oem1MyNameIs", "https://www.mynameis.com/rapper/repo", "Who is the first repo?",
                    "22", "0", "0", "ignore", "ddddddd2313aaaaabcccc111");
            List<String> fdroid0 = Arrays.asList("F-Droid", "https://f-droid.org/repo", "The official F-Droid repository. Applications in this repository are mostly built directory from the source code. Some are official binaries built by the original application developers - these will be replaced by source-built versions over time.",
                    "13", "1", "1", "ignore", "3082035e30820246a00302010202044c49cd00300d06092a864886f70d01010505003071310b300906035504061302554b3110300e06035504081307556e6b6e6f776e3111300f0603550407130857657468657262793110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e311930170603550403131043696172616e2047756c746e69656b73301e170d3130303732333137313032345a170d3337313230383137313032345a3071310b300906035504061302554b3110300e06035504081307556e6b6e6f776e3111300f0603550407130857657468657262793110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e311930170603550403131043696172616e2047756c746e69656b7330820122300d06092a864886f70d01010105000382010f003082010a028201010096d075e47c014e7822c89fd67f795d23203e2a8843f53ba4e6b1bf5f2fd0e225938267cfcae7fbf4fe596346afbaf4070fdb91f66fbcdf2348a3d92430502824f80517b156fab00809bdc8e631bfa9afd42d9045ab5fd6d28d9e140afc1300917b19b7c6c4df4a494cf1f7cb4a63c80d734265d735af9e4f09455f427aa65a53563f87b336ca2c19d244fcbba617ba0b19e56ed34afe0b253ab91e2fdb1271f1b9e3c3232027ed8862a112f0706e234cf236914b939bcf959821ecb2a6c18057e070de3428046d94b175e1d89bd795e535499a091f5bc65a79d539a8d43891ec504058acb28c08393b5718b57600a211e803f4a634e5c57f25b9b8c4422c6fd90203010001300d06092a864886f70d0101050500038201010008e4ef699e9807677ff56753da73efb2390d5ae2c17e4db691d5df7a7b60fc071ae509c5414be7d5da74df2811e83d3668c4a0b1abc84b9fa7d96b4cdf30bba68517ad2a93e233b042972ac0553a4801c9ebe07bf57ebe9a3b3d6d663965260e50f3b8f46db0531761e60340a2bddc3426098397fda54044a17e5244549f9869b460ca5e6e216b6f6a2db0580b480ca2afe6ec6b46eedacfa4aa45038809ece0c5978653d6c85f678e7f5a2156d1bedd8117751e64a4b0dcd140f3040b021821a8d93aed8d01ba36db6c82372211fed714d9a32607038cdfd565bd529ffc637212aaa2c224ef22b603eccefb5bf1e085c191d4b24fe742b17ab3f55d4e6f05ef");
            List<String> fdroid1 = Arrays.asList("F-Droid Archive", "https://f-droid.org/archive", "The archive repository of the F-Droid client. This contains older versions of applications from the main repository.",
                    "13", "0", "2", "ignore", "3082035e30820246a00302010202044c49cd00300d06092a864886f70d01010505003071310b300906035504061302554b3110300e06035504081307556e6b6e6f776e3111300f0603550407130857657468657262793110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e311930170603550403131043696172616e2047756c746e69656b73301e170d3130303732333137313032345a170d3337313230383137313032345a3071310b300906035504061302554b3110300e06035504081307556e6b6e6f776e3111300f0603550407130857657468657262793110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e311930170603550403131043696172616e2047756c746e69656b7330820122300d06092a864886f70d01010105000382010f003082010a028201010096d075e47c014e7822c89fd67f795d23203e2a8843f53ba4e6b1bf5f2fd0e225938267cfcae7fbf4fe596346afbaf4070fdb91f66fbcdf2348a3d92430502824f80517b156fab00809bdc8e631bfa9afd42d9045ab5fd6d28d9e140afc1300917b19b7c6c4df4a494cf1f7cb4a63c80d734265d735af9e4f09455f427aa65a53563f87b336ca2c19d244fcbba617ba0b19e56ed34afe0b253ab91e2fdb1271f1b9e3c3232027ed8862a112f0706e234cf236914b939bcf959821ecb2a6c18057e070de3428046d94b175e1d89bd795e535499a091f5bc65a79d539a8d43891ec504058acb28c08393b5718b57600a211e803f4a634e5c57f25b9b8c4422c6fd90203010001300d06092a864886f70d0101050500038201010008e4ef699e9807677ff56753da73efb2390d5ae2c17e4db691d5df7a7b60fc071ae509c5414be7d5da74df2811e83d3668c4a0b1abc84b9fa7d96b4cdf30bba68517ad2a93e233b042972ac0553a4801c9ebe07bf57ebe9a3b3d6d663965260e50f3b8f46db0531761e60340a2bddc3426098397fda54044a17e5244549f9869b460ca5e6e216b6f6a2db0580b480ca2afe6ec6b46eedacfa4aa45038809ece0c5978653d6c85f678e7f5a2156d1bedd8117751e64a4b0dcd140f3040b021821a8d93aed8d01ba36db6c82372211fed714d9a32607038cdfd565bd529ffc637212aaa2c224ef22b603eccefb5bf1e085c191d4b24fe742b17ab3f55d4e6f05ef");
            List<String> fdroid2 = Arrays.asList("Guardian Project", "https://guardianproject.info/fdroid/repo", "The official app repository of The Guardian Project. Applications in this repository are official binaries build by the original application developers and signed by the same key as the APKs that are released in the Google Play store.",
                    "13", "0", "3", "ignore", "308205d8308203c0020900a397b4da7ecda034300d06092a864886f70d01010505003081ad310b30090603550406130255533111300f06035504080c084e657720596f726b3111300f06035504070c084e657720596f726b31143012060355040b0c0b4644726f6964205265706f31193017060355040a0c10477561726469616e2050726f6a656374311d301b06035504030c14677561726469616e70726f6a6563742e696e666f3128302606092a864886f70d0109011619726f6f7440677561726469616e70726f6a6563742e696e666f301e170d3134303632363139333931385a170d3431313131303139333931385a3081ad310b30090603550406130255533111300f06035504080c084e657720596f726b3111300f06035504070c084e657720596f726b31143012060355040b0c0b4644726f6964205265706f31193017060355040a0c10477561726469616e2050726f6a656374311d301b06035504030c14677561726469616e70726f6a6563742e696e666f3128302606092a864886f70d0109011619726f6f7440677561726469616e70726f6a6563742e696e666f30820222300d06092a864886f70d01010105000382020f003082020a0282020100b3cd79121b9b883843be3c4482e320809106b0a23755f1dd3c7f46f7d315d7bb2e943486d61fc7c811b9294dcc6b5baac4340f8db2b0d5e14749e7f35e1fc211fdbc1071b38b4753db201c314811bef885bd8921ad86facd6cc3b8f74d30a0b6e2e6e576f906e9581ef23d9c03e926e06d1f033f28bd1e21cfa6a0e3ff5c9d8246cf108d82b488b9fdd55d7de7ebb6a7f64b19e0d6b2ab1380a6f9d42361770d1956701a7f80e2de568acd0bb4527324b1e0973e89595d91c8cc102d9248525ae092e2c9b69f7414f724195b81427f28b1d3d09a51acfe354387915fd9521e8c890c125fc41a12bf34d2a1b304067ab7251e0e9ef41833ce109e76963b0b256395b16b886bca21b831f1408f836146019e7908829e716e72b81006610a2af08301de5d067c9e114a1e5759db8a6be6a3cc2806bcfe6fafd41b5bc9ddddb3dc33d6f605b1ca7d8a9e0ecdd6390d38906649e68a90a717bea80fa220170eea0c86fc78a7e10dac7b74b8e62045a3ecca54e035281fdc9fe5920a855fde3c0be522e3aef0c087524f13d973dff3768158b01a5800a060c06b451ec98d627dd052eda804d0556f60dbc490d94e6e9dea62ffcafb5beffbd9fc38fb2f0d7050004fe56b4dda0a27bc47554e1e0a7d764e17622e71f83a475db286bc7862deee1327e2028955d978272ea76bf0b88e70a18621aba59ff0c5993ef5f0e5d6b6b98e68b70203010001300d06092a864886f70d0101050500038202010079c79c8ef408a20d243d8bd8249fb9a48350dc19663b5e0fce67a8dbcb7de296c5ae7bbf72e98a2020fb78f2db29b54b0e24b181aa1c1d333cc0303685d6120b03216a913f96b96eb838f9bff125306ae3120af838c9fc07ebb5100125436bd24ec6d994d0bff5d065221871f8410daf536766757239bf594e61c5432c9817281b985263bada8381292e543a49814061ae11c92a316e7dc100327b59e3da90302c5ada68c6a50201bda1fcce800b53f381059665dbabeeb0b50eb22b2d7d2d9b0aa7488ca70e67ac6c518adb8e78454a466501e89d81a45bf1ebc350896f2c3ae4b6679ecfbf9d32960d4f5b493125c7876ef36158562371193f600bc511000a67bdb7c664d018f99d9e589868d103d7e0994f166b2ba18ff7e67d8c4da749e44dfae1d930ae5397083a51675c409049dfb626a96246c0015ca696e94ebb767a20147834bf78b07fece3f0872b057c1c519ff882501995237d8206b0b3832f78753ebd8dcbd1d3d9f5ba733538113af6b407d960ec4353c50eb38ab29888238da843cd404ed8f4952f59e4bbc0035fc77a54846a9d419179c46af1b4a3b7fc98e4d312aaa29b9b7d79e739703dc0fa41c7280d5587709277ffa11c3620f5fba985b82c238ba19b17ebd027af9424be0941719919f620dd3bb3c3f11638363708aa11f858e153cf3a69bce69978b90e4a273836100aa1e617ba455cd00426847f");
            List<String> fdroid3 = Arrays.asList("Guardian Project Archive", "https://guardianproject.info/fdroid/archive", "The official repository of The Guardian Project apps for use with F-Droid client. This contains older versions of applications from the main repository.",
                    "13", "0", "4", "ignore", "308205d8308203c0020900a397b4da7ecda034300d06092a864886f70d01010505003081ad310b30090603550406130255533111300f06035504080c084e657720596f726b3111300f06035504070c084e657720596f726b31143012060355040b0c0b4644726f6964205265706f31193017060355040a0c10477561726469616e2050726f6a656374311d301b06035504030c14677561726469616e70726f6a6563742e696e666f3128302606092a864886f70d0109011619726f6f7440677561726469616e70726f6a6563742e696e666f301e170d3134303632363139333931385a170d3431313131303139333931385a3081ad310b30090603550406130255533111300f06035504080c084e657720596f726b3111300f06035504070c084e657720596f726b31143012060355040b0c0b4644726f6964205265706f31193017060355040a0c10477561726469616e2050726f6a656374311d301b06035504030c14677561726469616e70726f6a6563742e696e666f3128302606092a864886f70d0109011619726f6f7440677561726469616e70726f6a6563742e696e666f30820222300d06092a864886f70d01010105000382020f003082020a0282020100b3cd79121b9b883843be3c4482e320809106b0a23755f1dd3c7f46f7d315d7bb2e943486d61fc7c811b9294dcc6b5baac4340f8db2b0d5e14749e7f35e1fc211fdbc1071b38b4753db201c314811bef885bd8921ad86facd6cc3b8f74d30a0b6e2e6e576f906e9581ef23d9c03e926e06d1f033f28bd1e21cfa6a0e3ff5c9d8246cf108d82b488b9fdd55d7de7ebb6a7f64b19e0d6b2ab1380a6f9d42361770d1956701a7f80e2de568acd0bb4527324b1e0973e89595d91c8cc102d9248525ae092e2c9b69f7414f724195b81427f28b1d3d09a51acfe354387915fd9521e8c890c125fc41a12bf34d2a1b304067ab7251e0e9ef41833ce109e76963b0b256395b16b886bca21b831f1408f836146019e7908829e716e72b81006610a2af08301de5d067c9e114a1e5759db8a6be6a3cc2806bcfe6fafd41b5bc9ddddb3dc33d6f605b1ca7d8a9e0ecdd6390d38906649e68a90a717bea80fa220170eea0c86fc78a7e10dac7b74b8e62045a3ecca54e035281fdc9fe5920a855fde3c0be522e3aef0c087524f13d973dff3768158b01a5800a060c06b451ec98d627dd052eda804d0556f60dbc490d94e6e9dea62ffcafb5beffbd9fc38fb2f0d7050004fe56b4dda0a27bc47554e1e0a7d764e17622e71f83a475db286bc7862deee1327e2028955d978272ea76bf0b88e70a18621aba59ff0c5993ef5f0e5d6b6b98e68b70203010001300d06092a864886f70d0101050500038202010079c79c8ef408a20d243d8bd8249fb9a48350dc19663b5e0fce67a8dbcb7de296c5ae7bbf72e98a2020fb78f2db29b54b0e24b181aa1c1d333cc0303685d6120b03216a913f96b96eb838f9bff125306ae3120af838c9fc07ebb5100125436bd24ec6d994d0bff5d065221871f8410daf536766757239bf594e61c5432c9817281b985263bada8381292e543a49814061ae11c92a316e7dc100327b59e3da90302c5ada68c6a50201bda1fcce800b53f381059665dbabeeb0b50eb22b2d7d2d9b0aa7488ca70e67ac6c518adb8e78454a466501e89d81a45bf1ebc350896f2c3ae4b6679ecfbf9d32960d4f5b493125c7876ef36158562371193f600bc511000a67bdb7c664d018f99d9e589868d103d7e0994f166b2ba18ff7e67d8c4da749e44dfae1d930ae5397083a51675c409049dfb626a96246c0015ca696e94ebb767a20147834bf78b07fece3f0872b057c1c519ff882501995237d8206b0b3832f78753ebd8dcbd1d3d9f5ba733538113af6b407d960ec4353c50eb38ab29888238da843cd404ed8f4952f59e4bbc0035fc77a54846a9d419179c46af1b4a3b7fc98e4d312aaa29b9b7d79e739703dc0fa41c7280d5587709277ffa11c3620f5fba985b82c238ba19b17ebd027af9424be0941719919f620dd3bb3c3f11638363708aa11f858e153cf3a69bce69978b90e4a273836100aa1e617ba455cd00426847f");

            List<String> shouldBeRepos = new LinkedList<>();
            shouldBeRepos.addAll(oem0);
            shouldBeRepos.addAll(oem1);
            shouldBeRepos.addAll(fdroid0);
            shouldBeRepos.addAll(fdroid1);
            shouldBeRepos.addAll(fdroid2);
            shouldBeRepos.addAll(fdroid3);

            for (int i = 0; i < initialRepos.size(); i++) {
                assertEquals(shouldBeRepos.get(i), initialRepos.get(i));
            }
        } finally {
            for (Repo repo : RepoProvider.Helper.all(context, new String[]{Schema.RepoTable.Cols._ID})) {
                RepoProvider.Helper.remove(context, repo.getId());
            }
            additionalReposXmlFile.delete();
            DBHelper.clearDbHelperSingleton();
        }
    }
}