package com.dd.db;

import static com.dd.db.Tables.*;
import static com.dd.tools.Tools.ALPHA_ID;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Request;
import spark.Response;

import com.dd.DataSources;
import com.dd.db.Tables.Ballot;
import com.dd.db.Tables.Discussion;
import com.dd.db.Tables.Poll;
import com.dd.db.Tables.User;
import com.dd.db.Tables.UserView;
import com.dd.tools.Tools;
import com.dd.voting.ballot.RangeBallot;
import com.dd.voting.candidate.RangeCandidate;

// http://ondras.zarovi.cz/sql/demo/?keyword=dd_tyhou

public class Actions {

	static final Logger log = LoggerFactory.getLogger(Actions.class);

	public static String createEmptyPoll(String userId) {

		// First create a discussion
		Discussion d = DISCUSSION.createIt();

		Poll p = POLL.createIt("discussion_id", d.getId().toString(),
				"user_id", userId,
				"poll_type_id", 1);

		return p.getId().toString();
	}

	public static String savePoll(String userId, String pollId, String subject, String text, String password) {

		Poll p = POLL.findFirst("id = ? and user_id = ?", pollId, userId);

		if (p == null) {
			throw new NoSuchElementException("Wrong User");
		}

		p.set("private_password", password).saveIt();


		Discussion d = DISCUSSION.findFirst("id = ?", p.getString("discussion_id"));
		d.set("subject", subject,
				"text", text).saveIt();

		return "Poll Saved";


	}

	public static String createCandidate(String userId, String pollId, String subject, String text) {

		// First create a discussion
		Discussion d = DISCUSSION.createIt("subject", subject,
				"text", text);

		CANDIDATE.createIt("poll_id", pollId,
				"discussion_id", d.getId().toString(),
				"user_id", userId);

		return "Candidate created";

	}

	public static String saveBallot(String userId, String pollId, String candidateId,
			String rank) {

		String message = null;
		// fetch the vote if it exists
		Ballot b = BALLOT.findFirst("poll_id = ? and user_id = ? and candidate_id = ?", 
				pollId, 
				userId,
				candidateId);

		if (b == null) {
			if (rank != null) {
				b = BALLOT.createIt(
						"poll_id", pollId,
						"user_id", userId,
						"candidate_id", candidateId,
						"rank", rank);
				message = "Ballot Created";
			} else {
				message = "Ballot not created";
			}
		} else {
			if (rank != null) {
				b.set("rank", rank).saveIt();
				message = "Ballot updated";
			}
			// If the rank is null, then delete the ballot
			else {
				b.delete();
				message = "Ballot deleted";
			}
		}

		return message;

	}

	


	public static String setCookiesForLogin(UserView uv, Response res) {

		Integer expireSeconds = DataSources.EXPIRE_SECONDS;

		//		long now = new Date().getTime();

		//		long expireTime = now + expireSeconds*1000;

		//		Timestamp expireTS = new Timestamp(expireTime);


		// Not sure if this is necessary yet
		Boolean secure = false;

		// Set some cookies for that users login
		//		res.cookie("auth", auth, expireSeconds, secure);
		res.cookie("uid", uv.getId().toString(), expireSeconds, secure);
		//		res.cookie("user_name", client.getString("name"), expireSeconds, secure);

		return "Logged in";

	}

	public static UserView getUserFromCookie(Request req, Response res) {

		String uid = req.cookie("uid");

		UserView uv = null;
		BigInteger id = null;

		Tools.dbInit();
		// If no cookie, fetch user by ip address
		if (uid == null) {
			uv = USER_VIEW.findFirst("ip_address = ?", req.ip());

			if (uv != null) {
				Actions.setCookiesForLogin(uv, res);
				return uv;
			}

		} else {
			id = ALPHA_ID.decode(uid);
			uv = USER_VIEW.findFirst("id = ?" , id);

		}

		if (uv == null) {
			User user = USER.createIt("ip_address", req.ip());
			uv = USER_VIEW.findFirst("id = ?", user.getId());
			Actions.setCookiesForLogin(uv, res);
		}
		Tools.dbClose();

		return uv;
	}
	
	public static List<RangeBallot> convertDBBallots(String pollId) {
		List<RangeBallot> ballots = new ArrayList<>();
		
		Tools.dbInit();
		List<Ballot> dbBallots = BALLOT.find("poll_id = ?", pollId);
		for (Ballot dbBallot : dbBallots) {
			Integer candidateId = dbBallot.getInteger("candidate_id");
			Double rank = dbBallot.getDouble("rank");
			RangeBallot rb = new RangeBallot(new RangeCandidate(candidateId, rank));
			ballots.add(rb);
		}
		Tools.dbClose();
		
		return ballots;
	}
	
	public static String runRangeElection(String pollId) {
		
	}



}
