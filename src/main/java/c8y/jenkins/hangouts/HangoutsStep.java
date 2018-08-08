package c8y.jenkins.hangouts;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.google.api.services.chat.v1.model.User;
import com.google.common.collect.ImmutableSet;

import hudson.Extension;
import hudson.model.Run;
import hudson.util.FormValidation;

public class HangoutsStep extends Step implements Serializable {

	private static final long serialVersionUID = -4772889830814883958L;
	private static final Logger LOGGER = Logger.getLogger(HangoutsStep.class.getName());
	public static final int MAX_ENTRIES = 10;

	private String room;
	private String message;

	@DataBoundConstructor
	public HangoutsStep(@Nonnull String room) {
		this.room = room;
	}

	public String getRoom() {
		return room;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new Execution(this, context);
	}

	public static class Execution extends SynchronousStepExecution<Void> {

		private static final long serialVersionUID = 6872125878600187425L;

		private HangoutsStep step;
		private Run<?,?> run;

		protected Execution(HangoutsStep step, StepContext context) {
			super(context);
			this.step = step;

			try {
				this.run = context.get(Run.class);
			} catch (IOException e) {
				LOGGER.error(e);
			} catch (InterruptedException e) {
				LOGGER.error(e);
			}
		}

		@Override
		protected Void run() throws Exception {
			HangoutsSpace space = new HangoutsSpace(step.getRoom());
			Set<User> members = space.getMembers();

			RunReporter reporter = new RunReporter(run);
			String report = reporter.report(members);
			
			if (step.getMessage() != null && step.getMessage().length() > 0) {
				space.send(step.getMessage());
			}
			
			if (report != null && report.length() > 0) {
				space.send(report);
			}

			return null;
		}
	}

	@Extension
	public static class DescriptorImpl extends StepDescriptor {

		@Override
		public String getFunctionName() {
			return "chat";
		}

		@Override
		public String getDisplayName() {
			return Messages.HangoutsBuilder_DescriptorImpl_DisplayName();
		}

		public FormValidation doCheckRoom(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error(Messages.HangoutsBuilder_DescriptorImpl_errors_missingRoom());
			if (value.length() < 4)
				return FormValidation.warning(Messages.HangoutsBuilder_DescriptorImpl_warnings_tooShort());
			return FormValidation.ok();
		}

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			return ImmutableSet.of(Run.class);
		}
	}
}