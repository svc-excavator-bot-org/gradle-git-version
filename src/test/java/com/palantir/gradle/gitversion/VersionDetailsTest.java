package com.palantir.gradle.gitversion;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionDetailsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private Git git;
    private PersonIdent identity = new PersonIdent("name", "email@address",
            new Date(1234L), TimeZone.getTimeZone("UTC"));

    @Before
    public void before() throws GitAPIException {
        git = Git.init().setDirectory(temporaryFolder.getRoot()).call();
    }

    @Test
    public void symlinks_should_result_in_clean_git_tree() throws Exception {
        File fileToLinkTo = write(temporaryFolder.newFile("fileToLinkTo"));
        Files.createSymbolicLink(temporaryFolder.getRoot().toPath().resolve("fileLink"), fileToLinkTo.toPath());

        File folderToLinkTo = temporaryFolder.newFolder("folderToLinkTo");
        write(new File(folderToLinkTo, "dummyFile"));
        Files.createSymbolicLink(temporaryFolder.getRoot().toPath().resolve("folderLink"), folderToLinkTo.toPath());

        git.add().addFilepattern(".").call();
        git.commit().setMessage("initial commit").call();
        git.tag().setAnnotated(true).setMessage("unused").setName("1.0.0").call();

        assertThat(versionDetails().getVersion()).isEqualTo("1.0.0");
    }

    @Test
    public void short_sha_when_no_annotated_tags_are_present() throws Exception {
        git.add().addFilepattern(".").call();
        git.commit().setAuthor(identity).setCommitter(identity).setMessage("initial commit").call();

        assertThat(versionDetails().getVersion()).isEqualTo("6f0c7ed");
    }

    @Test
    public void short_sha_when_no_annotated_tags_are_present_and_dirty_content() throws Exception {
        git.add().addFilepattern(".").call();
        git.commit().setAuthor(identity).setCommitter(identity).setMessage("initial commit").call();
        write(temporaryFolder.newFile("foo"));

        assertThat(versionDetails().getVersion()).isEqualTo("6f0c7ed.dirty");
    }

    private File write(File file) throws IOException {
        Files.write(file.toPath(), "content".getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private VersionDetails versionDetails() {
        return new VersionDetails(git, new GitVersionArgs());
    }
}
