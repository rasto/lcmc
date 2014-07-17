Linux Cluster Management Console (LCMC)
http://lcmc.sf.net
=======================================
LCMC is a Java GUI application that configures, manages and visualizes Linux
clusters. Specifically it helps administrators to create and manage clusters
that use one or more of these components: Pacemaker, Corosync, Heartbeat,
DRBD, KVM, XEN and LVM.

installation from source
========================
to compile and run type: mvn package

you may need to install these packages: libxalan2-java, libjava3d-java
and provide jai_core.jar in build-lib/ directory

installation on Debian/Ubuntu etc. 
==================================
download .deb package and install it with dpkg -i lcmc-....deb,
type "lcmc" to run it, or run it from menu.

installation on Redhat/Fedora/Suse etc.
=======================================
rpm -Uvh lcmc-*.rpm
type "lcmc" to run it, or run it from menu.

sudoers file
============
for sudo you would need at least this commands in the sudoers file:

rasto ALL=SETENV: /usr/local/bin/lcmc-gui-helper-*, /sbin/drbdadm,
/usr/sbin/cibadmin, /usr/sbin/crm_resource, /usr/sbin/crm_attribute,
/usr/sbin/ptest, /usr/sbin/crm_simulate, /usr/sbin/crm_mon,
/usr/sbin/crm, /usr/sbin/crm_verify, /usr/sbin/corosync-cfgtool,
/usr/sbin/corosync-quorumtool, /usr/bin/cl_status

Some things like creating of VM and DRBD config files wouldn't work
unless you allow the "bash" command. A user may not be able to see
log files in the GUI if he doesn't have permissions to read them. 

author
======
Rasto Levrinc
