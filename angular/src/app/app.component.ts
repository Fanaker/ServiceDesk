import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ServiceDeskApi, Workspace, SupportTicket, TicketStatus, DemoRole, Priority } from './service-desk.api';

@Component({
 selector:'app-root', standalone:true,imports:[CommonModule,FormsModule],
 templateUrl:'./app.component.html'
})
export class AppComponent implements OnInit {
 private readonly api=inject(ServiceDeskApi);
 workspace:Workspace|null=null;
 view='Overview';
 readonly navigation=['Overview','Tickets','SLA Center','Team','Knowledge','Reports'];
 role:DemoRole='Admin';
 user='Diego Flores';
 filter='';statusFilter='All'; priorityFilter='All';
 selected:SupportTicket|null=null;
 createOpen=false;busy=false;error='';message='';
 form={title:'',description:'',category:'Applications',priority:'Medium'};
 commentText='';
 selectedStatus:TicketStatus='Open'; selectedAssignee='Unassigned';
 ngOnInit(){this.reload();}
 reload(){this.busy=true;this.api.bootstrap().subscribe({next:(data)=>{this.workspace=data;this.busy=false;if(this.selected){this.selected=data.tickets.find(t=>t.id===this.selected?.id)||null;}},error:(error)=>{this.error=`Unable to load service workspace: ${error.message}`;this.busy=false;}})}
 roleChange(role:DemoRole){this.role=role;this.user=role==='Admin'?'Diego Flores':role==='Technician'?'Lucía Ramos':'Ana Paredes';}
 get tickets():SupportTicket[]{return this.workspace?.tickets||[];}
 get active(){return this.tickets.filter(t=>!['Resolved','Closed'].includes(t.status));}
 get critical(){return this.active.filter(t=>t.priority==='Urgent');}
 get resolved(){return this.tickets.filter(t=>['Resolved','Closed'].includes(t.status));}
 get resolutionRate(){return this.tickets.length?Math.round(100*this.resolved.length/this.tickets.length):0;}
 get filtered():SupportTicket[]{const q=this.filter.toLowerCase();return this.tickets.filter(t=>
   (!q||[t.title,t.description,t.requester,t.category,`SD-${t.id}`].join(' ').toLowerCase().includes(q)) &&
   (this.statusFilter==='All'||t.status===this.statusFilter) &&
   (this.priorityFilter==='All'||t.priority===this.priorityFilter));}
 get priorityQueue(){const order:Priority[]=['Urgent','High','Medium','Low'];return [...this.active].sort((a,b)=>order.indexOf(a.priority)-order.indexOf(b.priority)).slice(0,8);}
 get workload(){return (this.workspace?.users||[]).filter(u=>u.role==='Technician').map(u=>({...u,count:this.active.filter(t=>t.assignee===u.name).length}));}
 get categories(){return [...new Set(this.tickets.map(t=>t.category))].map(category=>({category,count:this.tickets.filter(t=>t.category===category).length})).sort((a,b)=>b.count-a.count);}
 select(t:SupportTicket){this.selected=t;this.selectedStatus=t.status;this.selectedAssignee=t.assignee;this.commentText='';}
 changeStatus(){if(!this.selected||this.role==='Requester')return;this.api.status(this.selected.id,this.selectedStatus,this.role,this.user).subscribe({next:()=>{this.message='Status updated';this.reload();},error:e=>this.error=e.error?.error||e.message});}
 assign(){if(!this.selected||this.role!=='Admin')return;this.api.assign(this.selected.id,this.selectedAssignee,this.role,this.user).subscribe({next:()=>{this.message='Assignment saved';this.reload();},error:e=>this.error=e.error?.error||e.message});}
 addComment(){if(!this.selected||this.commentText.trim().length<3)return;this.api.comment(this.selected.id,this.commentText,this.role,this.user).subscribe({next:()=>{this.commentText='';this.message='Comment added';this.reload();},error:e=>this.error=e.error?.error||e.message});}
 newTicket(){if(this.form.title.trim().length<5||this.form.description.trim().length<10)return;
  this.api.create(this.form,this.role,this.user).subscribe({next:()=>{this.createOpen=false;this.form={title:'',description:'',category:'Applications',priority:'Medium'};this.view='Tickets';this.message='Ticket created';this.reload();},error:e=>this.error=e.error?.error||e.message});}
 assignedTechnicians(){return (this.workspace?.users||[]).filter(u=>u.role==='Technician'||u.role==='Admin');}
 initials(name:string){return name.split(' ').slice(0,2).map(w=>w[0]).join('').toUpperCase();}
}
