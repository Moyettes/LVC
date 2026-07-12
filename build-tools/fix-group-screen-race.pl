#!/usr/bin/perl
# Eliminates a ConcurrentModificationException race in GroupMembersScreen
# and GroupsScreen. Both screens register a callback into VoiceClient so the
# server can push new member/group lists. That callback fires on VoiceClient's
# network reader thread and calls buildMemberEntries / buildGroupEntries,
# which mutate Screen.buttons — owned by the MC main thread. If a GUI init
# races with a callback, Iterator.next() on buttons throws CME.
#
# Fix: make the callback just stash the incoming list in a volatile field.
# Drain the stashed list at the top of renderContent (which runs on the MC
# main thread), applying it there.
#
# Usage: fix-group-screen-race.pl <GroupMembersScreen.java|GroupsScreen.java>
#        Detects which file it is by filename.

use strict;
use warnings;

die "usage: $0 <GroupMembersScreen.java|GroupsScreen.java>\n" unless @ARGV == 1;
my $path = $ARGV[0];
open my $in, '<', $path or die "open $path: $!";
local $/;
my $src = <$in>;
close $in;

# Already patched? Leave it alone.
if ($src =~ /drainPendingGroupMembers|drainPendingAvailableGroups/) {
	print "  (already patched) $path\n";
	exit 0;
}

my $kind;
if ($path =~ /GroupMembersScreen\.java$/) {
	$kind = 'members';
} elsif ($path =~ /GroupsScreen\.java$/) {
	$kind = 'groups';
} else {
	die "unexpected file name: $path\n";
}

if ($kind eq 'members') {
	# 1. Add volatile pending field after the groupMembers declaration.
	$src =~ s{
		(private\s+List<String>\s+groupMembers\s*=\s*new\s+ArrayList<String>\(\);)
	}{$1\n    private volatile java.util.List<String> pendingGroupMembers;}xs
		or die "$path: could not find groupMembers declaration\n";

	# 2. Rewrite updateMemberList(List<String>) to just stash the list.
	$src =~ s{
		public\s+void\s+updateMemberList\(List<String>\s+members\)\s*\{
			[^{}]*
		\}
	}{public void updateMemberList(List<String> members) {
        // Called from VoiceClient's network reader thread; buildMemberEntries
        // mutates Screen.buttons which is owned by the MC main thread.
        // Stash here and apply inside renderContent (main-thread driven).
        this.pendingGroupMembers = new java.util.ArrayList<String>(members);
    }

    private void drainPendingGroupMembers() {
        java.util.List<String> pending = this.pendingGroupMembers;
        if (pending == null) return;
        this.pendingGroupMembers = null;
        this.groupMembers = pending;
        buildMemberEntries();
    }}xs
		or die "$path: could not find updateMemberList method\n";

	# 3. Call drainPendingGroupMembers at the top of renderContent.
	$src =~ s{
		(protected\s+void\s+renderContent\([^)]*\)\s*\{)
	}{$1\n        drainPendingGroupMembers();}xs
		or die "$path: could not find renderContent method\n";
} else {
	# GroupsScreen variant — fields/methods named differently.
	# 1. Pending list field after availableGroups declaration.
	$src =~ s{
		(private\s+List<GroupListPacket\.GroupInfo>\s+availableGroups\s*=\s*new\s+ArrayList<[^>]*>\(\);)
	}{$1\n    private volatile java.util.List<GroupListPacket.GroupInfo> pendingAvailableGroups;}xs
		or die "$path: could not find availableGroups declaration\n";

	# 2. Rewrite updateGroupList.
	$src =~ s{
		public\s+void\s+updateGroupList\(List<GroupListPacket\.GroupInfo>\s+groups\)\s*\{
			[^{}]*
		\}
	}{public void updateGroupList(List<GroupListPacket.GroupInfo> groups) {
        // Called from VoiceClient's network reader thread; see GroupMembersScreen.
        this.pendingAvailableGroups = new java.util.ArrayList<GroupListPacket.GroupInfo>(groups);
    }

    private void drainPendingAvailableGroups() {
        java.util.List<GroupListPacket.GroupInfo> pending = this.pendingAvailableGroups;
        if (pending == null) return;
        this.pendingAvailableGroups = null;
        this.availableGroups = pending;
        buildGroupEntries();
    }}xs
		or die "$path: could not find updateGroupList method\n";

	# 3. Call drain at top of renderContent.
	$src =~ s{
		(protected\s+void\s+renderContent\([^)]*\)\s*\{)
	}{$1\n        drainPendingAvailableGroups();}xs
		or die "$path: could not find renderContent method\n";
}

open my $out, '>', $path or die "write $path: $!";
print $out $src;
close $out;
print "  patched $path\n";
