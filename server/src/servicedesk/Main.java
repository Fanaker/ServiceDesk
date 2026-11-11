package servicedesk;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/** Self-contained Java 21 reference service. Uses TSV for local persistence (no external dependencies). */
public final class Main {
    static final Path ROOT = Paths.get("").toAbsolutePath();
    static final Path DATA = ROOT.resolve("data/runtime");
    static final Path SEED = ROOT.resolve("data/seed");
    static final Path WEB = ROOT.resolve("web");
    static final List<Ticket> tickets = new ArrayList<>();
    static final List<Event> events = new ArrayList<>();
    static final List<Comment> comments = new ArrayList<>();
    static final String[] STATUSES={"Open","In progress","Resolved","Closed"};
    static final String[] PRIORITIES={"Low","Medium","High","Urgent"};
    static final String[][] USERS={{"Lucía Ramos","Technician","IT Support","LR"},{"Marco Vidal","Technician","Infrastructure","MV"},{"Valeria Cruz","Technician","Applications","VC"},{"Ana Paredes","Requester","People Ops","AP"},{"Luis Herrera","Requester","Finance","LH"},{"Sofía Medina","Requester","Operations","SM"},{"Diego Flores","Admin","IT Manager","DF"}};
    static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    static class Ticket {int id; String title,description,department,category,priority,status,requester,assignee,created,updated,due,asset;
        Ticket(String[] v){id=Integer.parseInt(v[0]);title=v[1];description=v[2];department=v[3];category=v[4];priority=v[5];status=v[6];requester=v[7];assignee=v[8];created=v[9];updated=v[10];due=v[11];asset=v[12];}
        String[] fields(){return new String[]{""+id,title,description,department,category,priority,status,requester,assignee,created,updated,due,asset};}
        String json(){return "{\"id\":"+id+",\"title\":"+q(title)+",\"description\":"+q(description)+",\"department\":"+q(department)+",\"category\":"+q(category)+",\"priority\":"+q(priority)+",\"status\":"+q(status)+",\"requester\":"+q(requester)+",\"assignee\":"+q(assignee)+",\"created\":"+q(created)+",\"updated\":"+q(updated)+",\"due\":"+q(due)+",\"asset\":"+q(asset)+"}";}
    }
    record Event(String time,int ticket,String actor,String action,String detail){String json(){return "{\"time\":"+q(time)+",\"ticket\":"+ticket+",\"actor\":"+q(actor)+",\"action\":"+q(action)+",\"detail\":"+q(detail)+"}";}}
    record Comment(int ticket,String author,String time,String message){String json(){return "{\"ticket\":"+ticket+",\"author\":"+q(author)+",\"time\":"+q(time)+",\"message\":"+q(message)+"}";}}
    static String q(String s){if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"'->b.append("\\\"");case '\\'->b.append("\\\\");case '\n'->b.append("\\n");case '\r'->b.append("\\r");case '\t'->b.append("\\t");default->{if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}}return b.append('"').toString();}
    static String arr(Collection<String> values){return "["+String.join(",",values)+"]";}
    static String now(){return LocalDateTime.now(ZoneOffset.UTC).format(DATE);}
    static String text(byte[] bytes){return new String(bytes,StandardCharsets.UTF_8);}
    static synchronized void load() throws IOException {
        Files.createDirectories(DATA);
        for(String name:List.of("tickets.tsv","events.tsv","comments.tsv")){Path dest=DATA.resolve(name);if(!Files.exists(dest))Files.copy(SEED.resolve(name),dest);}
        tickets.clear();events.clear();comments.clear();
        for(String line:Files.readAllLines(DATA.resolve("tickets.tsv"),StandardCharsets.UTF_8))if(!line.isBlank()){String[] v=line.split("\t",-1);if(v.length==13)tickets.add(new Ticket(v));}
        for(String line:Files.readAllLines(DATA.resolve("events.tsv"),StandardCharsets.UTF_8))if(!line.isBlank()){String[] v=line.split("\t",-1);if(v.length>=5)events.add(new Event(v[0],Integer.parseInt(v[1]),v[2],v[3],v[4]));}
        for(String line:Files.readAllLines(DATA.resolve("comments.tsv"),StandardCharsets.UTF_8))if(!line.isBlank()){String[] v=line.split("\t",-1);if(v.length>=4)comments.add(new Comment(Integer.parseInt(v[0]),v[1],v[2],v[3]));}
    }
    static String clean(String s){return (s==null?"":s).replaceAll("[\\t\\r\\n]+"," ").strip();}
    static synchronized void persist() throws IOException{
        List<String> rows=new ArrayList<>();for(Ticket t:tickets)rows.add(String.join("\t",Arrays.stream(t.fields()).map(Main::clean).toArray(String[]::new)));
        Files.write(DATA.resolve("tickets.tsv"),rows,StandardCharsets.UTF_8);
        List<String> evs=new ArrayList<>();for(Event e:events)evs.add(String.join("\t",clean(e.time()),""+e.ticket(),clean(e.actor()),clean(e.action()),clean(e.detail())));
        Files.write(DATA.resolve("events.tsv"),evs,StandardCharsets.UTF_8);
        List<String> cms=new ArrayList<>();for(Comment c:comments)cms.add(String.join("\t",""+c.ticket(),clean(c.author()),clean(c.time()),clean(c.message())));
        Files.write(DATA.resolve("comments.tsv"),cms,StandardCharsets.UTF_8);
    }
    static String usersJson(){List<String> a=new ArrayList<>();for(String[] u:USERS)a.add("{\"name\":"+q(u[0])+",\"role\":"+q(u[1])+",\"team\":"+q(u[2])+",\"initials\":"+q(u[3])+"}");return arr(a);}
    static String knowledgeJson(){return "["+
      "{\"id\":1,\"category\":\"Identity & Access\",\"title\":\"Reset your company password securely\",\"readTime\":\"4 min read\",\"views\":1248},"+
      "{\"id\":2,\"category\":\"VPN & Remote Access\",\"title\":\"Troubleshoot remote access and VPN connections\",\"readTime\":\"6 min read\",\"views\":986},"+
      "{\"id\":3,\"category\":\"Hardware\",\"title\":\"Request replacement equipment or peripherals\",\"readTime\":\"3 min read\",\"views\":803},"+
      "{\"id\":4,\"category\":\"Applications\",\"title\":\"Install approved software on your device\",\"readTime\":\"5 min read\",\"views\":771},"+
      "{\"id\":5,\"category\":\"Security\",\"title\":\"How to report suspicious emails and security alerts\",\"readTime\":\"4 min read\",\"views\":656}]";}
    static synchronized String bootstrap(){return "{\"tickets\":"+arr(tickets.stream().map(Ticket::json).toList())+",\"users\":"+usersJson()+",\"events\":"+arr(events.stream().sorted(Comparator.comparing(Event::time).reversed()).limit(70).map(Event::json).toList())+",\"comments\":"+arr(comments.stream().map(Comment::json).toList())+",\"knowledge\":"+knowledgeJson()+",\"generatedAt\":"+q(now())+"}";}
    static synchronized Ticket find(int id){return tickets.stream().filter(t->t.id==id).findFirst().orElse(null);}
    static Map<String,String> form(HttpExchange exchange)throws IOException{String b=text(exchange.getRequestBody().readAllBytes());Map<String,String> values=new HashMap<>();for(String pair:b.split("&")){if(!pair.isEmpty()){int pos=pair.indexOf('=');String key=URLDecoder.decode(pos<0?pair:pair.substring(0,pos),StandardCharsets.UTF_8);String val=URLDecoder.decode(pos<0?"":pair.substring(pos+1),StandardCharsets.UTF_8);values.put(key,val);}}return values;}
    static String f(Map<String,String> m,String k){return clean(m.getOrDefault(k,""));}
    static boolean among(String s,String[] allowed){return Arrays.asList(allowed).contains(s);}
    static void send(HttpExchange ex,int status,String type,String body)throws IOException{byte[] bytes=body.getBytes(StandardCharsets.UTF_8);ex.getResponseHeaders().set("Content-Type",type+"; charset=utf-8");ex.getResponseHeaders().set("Cache-Control","no-store");ex.getResponseHeaders().set("X-Content-Type-Options","nosniff");ex.getResponseHeaders().set("Access-Control-Allow-Origin","http://localhost:4200");ex.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type, X-Demo-Role, X-Demo-User");ex.getResponseHeaders().set("Access-Control-Allow-Methods","GET, POST, PATCH, OPTIONS");ex.sendResponseHeaders(status,bytes.length);try(OutputStream out=ex.getResponseBody()){out.write(bytes);}}
    static void json(HttpExchange ex,int code,String data)throws IOException{send(ex,code,"application/json",data);}
    static void error(HttpExchange ex,int code,String message)throws IOException{json(ex,code,"{\"error\":"+q(message)+"}");}
    static String role(HttpExchange ex){String role=ex.getRequestHeaders().getFirst("X-Demo-Role");return among(role,new String[]{"Requester","Technician","Admin"})?role:"Requester";}
    static String actor(HttpExchange ex){String user=ex.getRequestHeaders().getFirst("X-Demo-User");if(user!=null)for(String[] u:USERS)if(u[0].equals(user) && u[1].equals(role(ex)))return user;return "Demo user ("+role(ex)+")";}
    static synchronized void api(HttpExchange ex)throws IOException{
        String path=ex.getRequestURI().getPath();String method=ex.getRequestMethod();
        if(method.equals("OPTIONS")){send(ex,204,"text/plain","");return;}
        if(method.equals("GET")&&path.equals("/api/health")){json(ex,200,"{\"status\":\"ok\",\"service\":\"ServiceDesk\",\"persistence\":\"TSV local\"}");return;}
        if(method.equals("GET")&&path.equals("/api/bootstrap")){json(ex,200,bootstrap());return;}
        if(method.equals("GET")&&path.equals("/api/export")){
            StringBuilder csv=new StringBuilder("ID,Title,Priority,Status,Category,Assignee,Requester,Created,Due\n");
            for(Ticket t:tickets){String[] fields={"SD-"+t.id,t.title,t.priority,t.status,t.category,t.assignee,t.requester,t.created,t.due};csv.append(Arrays.stream(fields).map(Main::csv).collect(Collectors.joining(","))).append('\n');}
            ex.getResponseHeaders().set("Content-Disposition","attachment; filename=servicedesk-tickets.csv");send(ex,200,"text/csv",csv.toString());return;
        }
        if(method.equals("POST")&&path.equals("/api/reset")){
            if(!role(ex).equals("Admin")){error(ex,403,"Admin role required");return;}
            for(String s:List.of("tickets.tsv","events.tsv","comments.tsv"))Files.copy(SEED.resolve(s),DATA.resolve(s),StandardCopyOption.REPLACE_EXISTING);load();json(ex,200,"{\"ok\":true}");return;
        }
        if(method.equals("POST")&&path.equals("/api/tickets")){
            Map<String,String> m=form(ex);String title=f(m,"title"),description=f(m,"description"),category=f(m,"category"),priority=f(m,"priority"),requester=actor(ex);
            if(title.length()<5||title.length()>140||description.length()<10||!among(priority,PRIORITIES)){error(ex,400,"Title (5–140 characters), description (10+), and valid priority required");return;}
            if(category.isEmpty())category="General";int id=tickets.stream().mapToInt(t->t.id).max().orElse(2400)+1;String ts=now();String due=LocalDateTime.now(ZoneOffset.UTC).plusHours(switch(priority){case "Urgent"->8;case "High"->24;case "Medium"->72;default->168;}).format(DATE);
            Ticket t=new Ticket(new String[]{""+id,title,description,"Corporate IT",category,priority,"Open",requester,"Unassigned",ts,ts,due,"—"}); tickets.add(0,t);events.add(new Event(ts,id,requester,"Ticket created","Open"));persist();json(ex,201,t.json());return;
        }
        if(path.matches("/api/tickets/\\d+/(status|assign|comments)")){
            String[] parts=path.split("/");int id=Integer.parseInt(parts[3]);Ticket t=find(id);if(t==null){error(ex,404,"Ticket not found");return;}
            String operation=parts[4];Map<String,String> m=form(ex);String user=actor(ex),r=role(ex),timestamp=now();
            if(operation.equals("status")&&method.equals("PATCH")){
                if(r.equals("Requester")){error(ex,403,"Technician or Admin role required");return;}
                String next=f(m,"status");if(!among(next,STATUSES)){error(ex,400,"Invalid status");return;}
                if(t.status.equals("Closed")&&!r.equals("Admin")){error(ex,403,"Only Admin can reopen closed tickets");return;}
                if(t.status.equals(next)){json(ex,200,t.json());return;}
                t.status=next;t.updated=timestamp;events.add(new Event(timestamp,id,user,"Status changed",next));persist();json(ex,200,t.json());return;
            }
            if(operation.equals("assign")&&method.equals("PATCH")){
                if(!r.equals("Admin")){error(ex,403,"Admin role required for assignment");return;}
                String assign=f(m,"assignee");boolean valid=assign.equals("Unassigned");for(String[] u:USERS)if(u[0].equals(assign)&&!u[1].equals("Requester"))valid=true;
                if(!valid){error(ex,400,"Invalid assignee");return;}t.assignee=assign;t.updated=timestamp;events.add(new Event(timestamp,id,user,"Assigned to technician",assign));persist();json(ex,200,t.json());return;
            }
            if(operation.equals("comments")&&method.equals("POST")){
                String message=f(m,"message");if(message.length()<3||message.length()>1000){error(ex,400,"Comment must be 3–1000 characters");return;}
                comments.add(new Comment(id,user,timestamp,message));t.updated=timestamp;events.add(new Event(timestamp,id,user,"Comment added",message.length()>60?message.substring(0,60):message));persist();json(ex,201,"{\"ok\":true}");return;
            }
        }
        error(ex,404,"Endpoint not found");
    }
    static String csv(String s){return "\""+s.replace("\"","\"\"")+"\"";}
    static void staticFile(HttpExchange ex)throws IOException {
        if(!ex.getRequestMethod().equals("GET")){error(ex,405,"Method not allowed");return;}
        String path=URLDecoder.decode(ex.getRequestURI().getPath(),StandardCharsets.UTF_8);if(path.equals("/"))path="/index.html";
        Path target=WEB.resolve(path.substring(1)).normalize();if(!target.startsWith(WEB)||!Files.isRegularFile(target)){error(ex,404,"Page not found");return;}
        String file=target.getFileName().toString();String type=file.endsWith(".html")?"text/html":file.endsWith(".css")?"text/css":file.endsWith(".js")?"text/javascript":file.endsWith(".svg")?"image/svg+xml":"application/octet-stream";
        byte[] b=Files.readAllBytes(target);ex.getResponseHeaders().set("Content-Type",type+"; charset=utf-8");ex.getResponseHeaders().set("X-Content-Type-Options","nosniff");ex.sendResponseHeaders(200,b.length);try(OutputStream out=ex.getResponseBody()){out.write(b);}
    }
    public static void main(String[] args)throws Exception {
        load();int port=args.length>0?Integer.parseInt(args[0]):8080;HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",port),0);
        server.createContext("/api",ex->{try{api(ex);}catch(Exception err){err.printStackTrace();try{error(ex,500,"Internal server error");}catch(Exception ignored){}}});
        server.createContext("/",ex->{try{staticFile(ex);}catch(Exception err){err.printStackTrace();try{error(ex,500,"Unable to load resource");}catch(Exception ignored){}}});
        server.setExecutor(Executors.newFixedThreadPool(8));server.start();System.out.println("ServiceDesk running at http://localhost:"+port+" | " + tickets.size()+" seeded tickets");
    }
}
