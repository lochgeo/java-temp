package com.payroll.demo.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.util.List;
import java.util.stream.Collectors;

import com.payroll.demo.common.OrderNotFoundException;
import com.payroll.demo.data.OrderModelAssembler;
import com.payroll.demo.data.OrderRepository;
import com.payroll.demo.domain.Order;
import com.payroll.demo.domain.Status;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.mediatype.problem.Problem;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// tag::main[]
@RestController
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderModelAssembler assembler;

    OrderController(OrderRepository orderRepository, OrderModelAssembler assembler) {

        this.orderRepository = orderRepository;
        this.assembler = assembler;
    }

    @GetMapping("/orders")
    public CollectionModel<EntityModel<Order>> all() {

        List<EntityModel<Order>> orders = orderRepository.findAll().stream() //
                .map(assembler::toModel) //
                .collect(Collectors.toList());

        return CollectionModel.of(orders, //
                linkTo(methodOn(OrderController.class).all()).withSelfRel());
    }

    @GetMapping("/orders/{id}")
    public EntityModel<Order> one(@PathVariable Long id) {

        Order order = orderRepository.findById(id) //
                .orElseThrow(() -> new OrderNotFoundException(id));

        return assembler.toModel(order);
    }

    @PostMapping("/orders")
    ResponseEntity<EntityModel<Order>> newOrder(@RequestBody Order order) {

        order.setStatus(Status.IN_PROGRESS);
        Order newOrder = orderRepository.save(order);

        return ResponseEntity //
                .created(linkTo(methodOn(OrderController.class).one(newOrder.getId())).toUri()) //
                .body(assembler.toModel(newOrder));
    }
    // end::main[]

    // tag::delete[]
    @DeleteMapping("/orders/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id) {

        Order order = orderRepository.findById(id) //
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() == Status.IN_PROGRESS) {
            order.setStatus(Status.CANCELLED);
            return ResponseEntity.ok(assembler.toModel(orderRepository.save(order)));
        }

        return ResponseEntity //
                .status(HttpStatus.METHOD_NOT_ALLOWED) //
                .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HTTP_PROBLEM_DETAILS_JSON_VALUE) //
                .body(Problem.create() //
                        .withTitle("Method not allowed") //
                        .withDetail("You can't cancel an order that is in the " + order.getStatus() + " status"));
    }
    // end::delete[]

    // tag::complete[]
    @PutMapping("/orders/{id}/complete")
    public ResponseEntity<?> complete(@PathVariable Long id) {

        Order order = orderRepository.findById(id) //
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() == Status.IN_PROGRESS) {
            order.setStatus(Status.COMPLETED);
            return ResponseEntity.ok(assembler.toModel(orderRepository.save(order)));
        }

        return ResponseEntity //
                .status(HttpStatus.METHOD_NOT_ALLOWED) //
                .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HTTP_PROBLEM_DETAILS_JSON_VALUE) //
                .body(Problem.create() //
                        .withTitle("Method not allowed") //
                        .withDetail("You can't complete an order that is in the " + order.getStatus() + " status"));
    }
    // end::complete[]
}