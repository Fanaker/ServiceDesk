package com.fanaker.servicedesk;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController @RequestMapping("/api") @CrossOrigin(origins={"http://localhost:4200","http://127.0.0.1:4200"})
public class ApiController {
 final TicketRepository tickets;
 final CommentRepository comments;
 final EventRepository events;
 public ApiController(TicketRepository tickets,CommentRepository comments,EventRepository events){this.tickets=tickets;this.comments=comments;this.events=events;}
 private final String[][] users={{"Lucía Ramos","Technician","IT Support","LR"},{"Marco Vidal","Technician","Infrastructure","MV"},{"Valeria Cruz","Technician","Applications","VC"},{"Ana Paredes","Requester","People Ops","AP"},{"Luis Herrera","Requester","Finance","LH"},{"Sofía Medina","Requester","Operations","SM"},{"Diego Flores","Admin","IT Manager","DF"}};
 private String now(){return LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));}
 private String role(String r){return Arrays.asList("Admin","Technician","Requester").contains(r)?r:"Requester";}
 private void require(String role,String... allowed){if(!Arrays.asList(allowed).contains(role))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Insufficient role");}
 private String actor(String role,String name){return Arrays.stream(users).anyMatch(u->u[0].equals(name)&&u[1].equals(role))?name:"Demo user ("+role+")";}
 private SupportTicket find(Long id){return tickets.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Ticket not found"));}
 private void audit(Long ticket,String who,String action,String detail){events.save(new ServiceEvent(now(),ticket,who,action,detail));}
 private Map<String,String> map(String... values){Map<String,String> result=new LinkedHashMap<>();for(int i=0;i+1<values.length;i+=2)result.put(values[i],values[i+1]);return result;}
 @GetMapping("/health") public Map<String,String> health(){return map("status","ok","service","ServiceDesk","storage","JPA");}
 @GetMapping("/bootstrap") public Map<String,Object> bootstrap(){
  List<Map<String,String>> userItems=Arrays.stream(users).map(u->map("name",u[0],"role",u[1],"team",u[2],"initials",u[3])).toList();
  List<Map<String,Object>> knowledge=List.of(
   Map.of("id",1,"category","Identity & Access","title","Reset your company password securely","readTime","4 min read","views",1248),
   Map.of("id",2,"category","VPN & Remote Access","title","Troubleshoot remote access and VPN connections","readTime","6 min read","views",986),
   Map.of("id",3,"category","Hardware","title","Request replacement equipment or peripherals","readTime","3 min read","views",803));
  Map<String,Object> payload=new LinkedHashMap<>();payload.put("tickets",tickets.findAll());payload.put("users",userItems);payload.put("events",events.findAll().stream().sorted(Comparator.comparing((ServiceEvent e)->e.time).reversed()).toList());payload.put("comments",comments.findAll());payload.put("knowledge",knowledge);payload.put("generatedAt",now());return payload;
 }
 @PostMapping(value="/tickets",consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE) public ResponseEntity<SupportTicket> create(@RequestParam Map<String,String> input,@RequestHeader(value="X-Demo-Role",required=false)String demoRole,@RequestHeader(value="X-Demo-User",required=false)String demoName){
  String title=input.getOrDefault("title","").strip(),description=input.getOrDefault("description","").strip(),priority=input.getOrDefault("priority","Medium");
  if(title.length()<5||title.length()>140||description.length()<10||!Arrays.asList("Urgent","High","Medium","Low").contains(priority))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid ticket fields");
  SupportTicket t=new SupportTicket();t.id=tickets.findAll().stream().mapToLong(v->v.id).max().orElse(2400)+1;t.title=title;t.description=description;t.priority=priority;t.category=input.getOrDefault("category","General");t.department="Corporate IT";t.status="Open";t.requester=actor(role(demoRole),demoName);t.assignee="Unassigned";t.created=now();t.updated=t.created;t.asset="—";
  t.due=LocalDateTime.now(ZoneOffset.UTC).plusHours(switch(priority){case "Urgent"->8;case "High"->24;case "Medium"->72;default->168;}).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
  SupportTicket saved=tickets.save(t);audit(t.id,t.requester,"Ticket created","Open");return ResponseEntity.status(201).body(saved);
 }
 @PatchMapping(value="/tickets/{id}/status",consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE) public SupportTicket status(@PathVariable Long id,@RequestParam String status,@RequestHeader(value="X-Demo-Role",required=false)String demoRole,@RequestHeader(value="X-Demo-User",required=false)String demoName){
  String r=role(demoRole);require(r,"Technician","Admin");if(!Arrays.asList("Open","In progress","Resolved","Closed").contains(status))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown status");
  SupportTicket t=find(id);if(t.status.equals("Closed")&&!r.equals("Admin"))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only an Admin can reopen closed tickets");
  t.status=status;t.updated=now();audit(id,actor(r,demoName),"Status changed",status);return tickets.save(t);
 }
 @PatchMapping(value="/tickets/{id}/assign",consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE) public SupportTicket assign(@PathVariable Long id,@RequestParam String assignee,@RequestHeader(value="X-Demo-Role",required=false)String demoRole,@RequestHeader(value="X-Demo-User",required=false)String demoName){
  String r=role(demoRole);require(r,"Admin");boolean valid=assignee.equals("Unassigned")||Arrays.stream(users).anyMatch(u->u[0].equals(assignee)&&!u[1].equals("Requester"));if(!valid)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid assignee");
  SupportTicket t=find(id);t.assignee=assignee;t.updated=now();audit(id,actor(r,demoName),"Assigned to technician",assignee);return tickets.save(t);
 }
 @PostMapping(value="/tickets/{id}/comments",consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE) public ResponseEntity<Map<String,Boolean>> comment(@PathVariable Long id,@RequestParam String message,@RequestHeader(value="X-Demo-Role",required=false)String demoRole,@RequestHeader(value="X-Demo-User",required=false)String demoName){
  SupportTicket t=find(id);if(message.strip().length()<3||message.length()>1000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid comment");String who=actor(role(demoRole),demoName);comments.save(new ServiceComment(id,who,now(),message.strip()));t.updated=now();tickets.save(t);audit(id,who,"Comment added",message.substring(0,Math.min(60,message.length())));return ResponseEntity.status(201).body(Map.of("ok",true));
 }
 @GetMapping(value="/export",produces="text/csv") public ResponseEntity<String> export(){
  StringBuilder csv=new StringBuilder("ID,Title,Priority,Status,Assignee\n");for(SupportTicket t:tickets.findAll())csv.append("SD-").append(t.id).append(",\"").append(t.title.replace("\"","\"\"")).append("\",").append(t.priority).append(',').append(t.status).append(',').append(t.assignee).append('\n');
  return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=servicedesk-tickets.csv").body(csv.toString());
 }
}
