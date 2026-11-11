import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export type TicketStatus='Open'|'In progress'|'Resolved'|'Closed';
export type Priority='Low'|'Medium'|'High'|'Urgent';
export type DemoRole='Requester'|'Technician'|'Admin';
export interface SupportTicket {
  id:number; title:string; description:string; department:string; category:string;
  priority:Priority; status:TicketStatus; requester:string; assignee:string;
  created:string; updated:string; due:string; asset:string;
}
export interface TeamMember { name:string; role:DemoRole; team:string; initials:string; }
export interface TicketEvent {time:string;ticket:number;actor:string;action:string;detail:string;}
export interface TicketComment {ticket:number;author:string;time:string;message:string;}
export interface KnowledgeArticle {id:number;title:string;category:string;readTime:string;views:number;}
export interface Workspace {
 tickets:SupportTicket[];users:TeamMember[];events:TicketEvent[];
 comments:TicketComment[];knowledge:KnowledgeArticle[];generatedAt:string;
}
@Injectable({providedIn:'root'})
export class ServiceDeskApi {
 private readonly http=inject(HttpClient);
 private readonly base='/api';
 private headers(role:DemoRole,user:string){return new HttpHeaders({'X-Demo-Role':role,'X-Demo-User':user});}
 bootstrap():Observable<Workspace>{return this.http.get<Workspace>(`${this.base}/bootstrap`);}
 create(data:Record<string,string>,role:DemoRole,user:string):Observable<SupportTicket>{return this.http.post<SupportTicket>(`${this.base}/tickets`,new HttpParams({fromObject:data}),{headers:this.headers(role,user)});}
 status(id:number,status:TicketStatus,role:DemoRole,user:string):Observable<SupportTicket>{return this.http.patch<SupportTicket>(`${this.base}/tickets/${id}/status`,new HttpParams({fromObject:{status}}),{headers:this.headers(role,user)});}
 assign(id:number,assignee:string,role:DemoRole,user:string):Observable<SupportTicket>{return this.http.patch<SupportTicket>(`${this.base}/tickets/${id}/assign`,new HttpParams({fromObject:{assignee}}),{headers:this.headers(role,user)});}
 comment(id:number,message:string,role:DemoRole,user:string){return this.http.post(`${this.base}/tickets/${id}/comments`,new HttpParams({fromObject:{message}}),{headers:this.headers(role,user)});}
}
